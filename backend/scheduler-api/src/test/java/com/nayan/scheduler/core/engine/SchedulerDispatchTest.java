package com.nayan.scheduler.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.service.TaskSchedulerService;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;

class SchedulerDispatchTest {

    @Test
    void schedulerOnlyForwardsClaimedExecutions() {
        TaskExecutionStore store = mock(TaskExecutionStore.class);
        Executor executor = mock(Executor.class);
        Scheduler scheduler = new Scheduler(executor, store);
        TaskExecution first = execution(ExecutionStatus.IN_QUEUE);
        TaskExecution second = execution(ExecutionStatus.IN_QUEUE);
        when(store.claimDueExecutions()).thenReturn(List.of(first, second));

        scheduler.processScheduledExecutions();

        var order = inOrder(store, executor);
        order.verify(store).claimDueExecutions();
        order.verify(executor).addScheduledExecution(first);
        order.verify(executor).addScheduledExecution(second);
        verifyNoMoreInteractions(store, executor);
    }

    @Test
    void repeatedPollingDoesNotDispatchTwice() {
        TaskExecutionIMStore store = new TaskExecutionIMStore();
        TaskExecution due = execution(ExecutionStatus.PENDING);
        store.addTaskExecution(due);
        Executor executor = mock(Executor.class);
        Scheduler scheduler = new Scheduler(executor, store);

        scheduler.processScheduledExecutions();
        scheduler.processScheduledExecutions();

        var claimed = org.mockito.ArgumentCaptor.forClass(TaskExecution.class);
        verify(executor).addScheduledExecution(claimed.capture());
        assertThat(claimed.getValue().getTaskExecutionId()).isEqualTo(due.getTaskExecutionId());
        assertThat(claimed.getValue().getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
        assertThat(store.getTaskExecutionsForTask(due.getTaskId()).getFirst().getExecutionStatus())
                .isEqualTo(ExecutionStatus.IN_QUEUE);
    }

    @Test
    void failedDatabaseClaimDoesNotDispatchAnything() {
        TaskExecutionStore store = mock(TaskExecutionStore.class);
        Executor executor = mock(Executor.class);
        when(store.claimDueExecutions()).thenThrow(new IllegalStateException("Database unavailable"));

        assertThatThrownBy(() -> new Scheduler(executor, store).processScheduledExecutions())
                .hasMessage("Database unavailable");
        verifyNoInteractions(executor);
    }

    @Test
    void dispatchFailureDoesNotAbandonTheRestOfTheClaimedBatch() {
        TaskExecutionStore store = mock(TaskExecutionStore.class);
        Executor executor = mock(Executor.class);
        TaskExecution first = execution(ExecutionStatus.IN_QUEUE);
        TaskExecution second = execution(ExecutionStatus.IN_QUEUE);
        when(store.claimDueExecutions()).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("Dispatch failed")).when(executor).addScheduledExecution(first);

        new Scheduler(executor, store).processScheduledExecutions();

        verify(executor).addScheduledExecution(second);
    }

    @Test
    void startupOnlyPollsAndCannotBeStartedTwice() throws Exception {
        TaskExecutionStore store = mock(TaskExecutionStore.class);
        CountDownLatch polled = new CountDownLatch(1);
        when(store.claimDueExecutions()).thenAnswer(invocation -> {
            polled.countDown();
            Thread.currentThread().interrupt();
            return List.of();
        });
        TaskSchedulerService service = new TaskSchedulerService(mock(TaskStore.class), mock(TaskScheduleStore.class),
                store, 0);

        service.startScheduler();

        assertThat(polled.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store).claimDueExecutions();
        verifyNoMoreInteractions(store);
        assertThatThrownBy(service::startScheduler).isInstanceOf(IllegalStateException.class);
    }

    private TaskExecution execution(ExecutionStatus status) {
        TaskExecution execution = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        execution.setExecutionStatus(status);
        execution.setWorkerId(status == ExecutionStatus.ASSIGNED ? 3 : -1);
        return execution;
    }
}
