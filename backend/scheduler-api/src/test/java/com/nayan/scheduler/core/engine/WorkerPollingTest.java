package com.nayan.scheduler.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;

class WorkerPollingTest {
    @Test
    void rejectedQueuedCopyDoesNotKillWorkerAndSuccessfulRunKeepsWorkerId() throws Exception {
        TaskExecutionIMStore store = spy(new TaskExecutionIMStore());
        TaskExecution discarded = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        TaskExecution execution = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        store.addTaskExecution(discarded);
        store.addTaskExecution(execution);
        var claimed = store.claimDueExecutions();
        store.discardExecutionsForTask(discarded.getTaskId());
        Queue<TaskExecution> queue = new LinkedList<>();
        queue.add(claimed.stream().filter(item -> item.getTaskExecutionId().equals(discarded.getTaskExecutionId()))
                .findFirst().orElseThrow());
        queue.add(claimed.stream().filter(item -> item.getTaskExecutionId().equals(execution.getTaskExecutionId()))
                .findFirst().orElseThrow());
        TaskStore taskStore = mock(TaskStore.class);
        Task task = mock(Task.class);
        when(taskStore.getTask(execution.getTaskId())).thenReturn(task);
        doAnswer(invocation -> {
            TaskExecution assigned = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
            assertThat(assigned.getExecutionStatus()).isEqualTo(ExecutionStatus.ASSIGNED);
            assertThat(assigned.getWorkerId()).isEqualTo(4);
            assertThat(Thread.holdsLock(queue)).isFalse();
            return null;
        }).when(task).execute();
        CountDownLatch finished = observeCompletion(store);
        Thread thread = new Thread(new Worker(queue, 4, taskStore, store), "worker-polling-test");
        thread.setDaemon(true);
        thread.start();
        try {
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
            TaskExecution completed = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
            assertThat(completed.getExecutionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
            assertThat(completed.getWorkerId()).isEqualTo(4);
            assertThat(store.getTaskExecutionsForTask(discarded.getTaskId()).getFirst().getExecutionStatus())
                    .isEqualTo(ExecutionStatus.DISCARDED);
            verify(task).execute();
        } finally {
            thread.interrupt();
            thread.join(5000);
        }
    }

    @Test
    void failingTaskStillRecordsFailedAfterThreeAttempts() throws Exception {
        TaskExecutionIMStore store = spy(new TaskExecutionIMStore());
        TaskExecution execution = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        store.addTaskExecution(execution);
        Queue<TaskExecution> queue = new LinkedList<>(store.claimDueExecutions());
        TaskStore taskStore = mock(TaskStore.class);
        Task task = mock(Task.class);
        when(taskStore.getTask(execution.getTaskId())).thenReturn(task);
        doThrow(new IllegalStateException("Expected task failure")).when(task).execute();
        CountDownLatch finished = observeCompletion(store);
        Thread thread = new Thread(new Worker(queue, 2, taskStore, store), "worker-failure-test");
        thread.setDaemon(true);
        thread.start();
        try {
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
            TaskExecution failed = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
            assertThat(failed.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
            assertThat(failed.getWorkerId()).isEqualTo(2);
            verify(task, times(3)).execute();
        } finally {
            thread.interrupt();
            thread.join(5000);
        }
    }

    private CountDownLatch observeCompletion(TaskExecutionIMStore store) {
        CountDownLatch finished = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            finished.countDown();
            return result;
        }).when(store).updateTaskExecution(any(TaskExecution.class));
        return finished;
    }
}
