package com.nayan.scheduler.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;

class InMemoryExecutionQueueTest {
    @Test
    void updatesAndReplacementAddsDoNotLeaveOldPriorityQueueEntries() {
        TaskExecutionIMStore store = new TaskExecutionIMStore();
        TaskExecution original = execution(Instant.EPOCH);
        store.addTaskExecution(original);
        TaskExecution replacement = new TaskExecution(original.getTaskExecutionId(), original.getTaskId(),
                original.getTaskScheduleId(), Instant.parse("2099-01-01T00:00:00Z"), -1, ExecutionStatus.PENDING);
        store.addTaskExecution(replacement);

        assertThat(store.claimDueExecutions()).isEmpty();

        TaskExecution updated = new TaskExecution(original.getTaskExecutionId(), original.getTaskId(),
                original.getTaskScheduleId(), Instant.EPOCH, -1, ExecutionStatus.PENDING);
        store.updateTaskExecution(updated);
        assertThat(store.claimDueExecutions()).singleElement()
                .satisfies(claimed -> assertThat(claimed.getTaskExecutionId()).isEqualTo(original.getTaskExecutionId()));
        assertThat(store.claimDueExecutions()).isEmpty();
    }

    @Test
    void callerMutationsCannotAlterStoredStateOrPriorityQueue() {
        TaskExecutionIMStore store = new TaskExecutionIMStore();
        TaskExecution execution = execution(Instant.EPOCH);
        TaskExecution added = store.addTaskExecution(execution);
        execution.setExecutionStatus(ExecutionStatus.COMPLETED);
        added.setExecutionStatus(ExecutionStatus.DISCARDED);
        store.getTaskExecutionsForTask(execution.getTaskId()).getFirst().setExecutionStatus(ExecutionStatus.FAILED);
        store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING).getFirst().setExecutionStatus(ExecutionStatus.ASSIGNED);

        List<TaskExecution> claimed = store.claimDueExecutions();

        assertThat(claimed).hasSize(1);
        assertThat(claimed.getFirst().getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
        claimed.getFirst().setExecutionStatus(ExecutionStatus.PENDING);
        assertThat(store.claimDueExecutions()).isEmpty();
        assertThat(store.getTaskExecutionsForTask(execution.getTaskId()).getFirst().getExecutionStatus())
                .isEqualTo(ExecutionStatus.IN_QUEUE);
    }

    @Test
    void cancelledAndCompletedRecordsAreRemovedFromPriorityQueue() {
        TaskExecutionIMStore store = new TaskExecutionIMStore();
        TaskExecution cancelled = execution(Instant.EPOCH);
        TaskExecution completed = execution(Instant.EPOCH);
        TaskExecution pending = execution(Instant.EPOCH);
        store.addTaskExecution(cancelled);
        store.addTaskExecution(completed);
        store.addTaskExecution(pending);
        store.discardExecutionsForTask(cancelled.getTaskId());
        completed.setExecutionStatus(ExecutionStatus.COMPLETED);
        store.updateTaskExecution(completed);

        assertThat(store.claimDueExecutions()).extracting(TaskExecution::getTaskExecutionId)
                .containsExactly(pending.getTaskExecutionId());
    }

    @Test
    void concurrentPollersClaimEachExecutionOnlyOnce() throws Exception {
        TaskExecutionIMStore store = new TaskExecutionIMStore();
        for (int i = 0; i < 50; i++) {
            store.addTaskExecution(execution(Instant.EPOCH.plusSeconds(i)));
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<List<TaskExecution>> poll = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for concurrent pollers");
                }
                return store.claimDueExecutions();
            };
            var first = pool.submit(poll);
            var second = pool.submit(poll);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<TaskExecution> all = Stream.concat(first.get(5, TimeUnit.SECONDS).stream(),
                    second.get(5, TimeUnit.SECONDS).stream()).toList();
            assertThat(all).hasSize(50);
            assertThat(all).extracting(TaskExecution::getTaskExecutionId).doesNotHaveDuplicates();
            assertThat(store.claimDueExecutions()).isEmpty();
        } finally {
            start.countDown();
        }
    }

    private TaskExecution execution(Instant executionTime) {
        return new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), executionTime);
    }
}
