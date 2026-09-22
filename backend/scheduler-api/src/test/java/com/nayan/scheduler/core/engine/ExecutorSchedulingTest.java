package com.nayan.scheduler.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;

class ExecutorSchedulingTest {
    @Test
    void recurringDispatchPersistsNextOccurrenceWithoutASchedulerQueue() {
        TaskStore tasks = mock(TaskStore.class);
        TaskScheduleStore schedules = mock(TaskScheduleStore.class);
        TaskExecutionIMStore executions = new TaskExecutionIMStore();
        UUID taskId = UUID.randomUUID();
        TaskSchedule schedule = new TaskSchedule(taskId, Instant.EPOCH, true, 3600);
        Task task = task(tasks, schedules, schedule, TaskStatus.ACTIVE);
        TaskExecution current = new TaskExecution(taskId, schedule.getTaskScheduleId(), Instant.EPOCH);
        executions.addTaskExecution(current);
        TaskExecution queued = executions.claimDueExecutions().getFirst();
        Instant beforeDispatch = Instant.now();

        new Executor(0, tasks, schedules, executions).addScheduledExecution(queued);

        assertThat(executions.getTaskExecutionsForTask(taskId)).hasSize(2);
        assertThat(executions.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING)).singleElement()
                .satisfies(next -> assertThat(next.getExecutionTime()).isAfterOrEqualTo(beforeDispatch.plusSeconds(3600)));
        verify(task, never()).setTaskStatus(TaskStatus.COMPLETED);
    }

    @Test
    void oneTimeDispatchPreservesExistingCompletionBehaviorWithoutCreatingAnotherExecution() {
        TaskStore tasks = mock(TaskStore.class);
        TaskScheduleStore schedules = mock(TaskScheduleStore.class);
        TaskExecutionIMStore executions = new TaskExecutionIMStore();
        UUID taskId = UUID.randomUUID();
        TaskSchedule schedule = new TaskSchedule(taskId, Instant.EPOCH, false, 0);
        Task task = task(tasks, schedules, schedule, TaskStatus.ACTIVE);
        executions.addTaskExecution(new TaskExecution(taskId, schedule.getTaskScheduleId(), Instant.EPOCH));

        new Executor(0, tasks, schedules, executions).addScheduledExecution(executions.claimDueExecutions().getFirst());

        assertThat(executions.getTaskExecutionsForTask(taskId)).hasSize(1);
        verify(task).setTaskStatus(TaskStatus.COMPLETED);
        verify(tasks).updateTask(task);
    }

    @Test
    void pausedTaskIsDiscardedEvenIfAlreadyClaimedByPoller() {
        TaskStore tasks = mock(TaskStore.class);
        TaskScheduleStore schedules = mock(TaskScheduleStore.class);
        TaskExecutionIMStore executions = new TaskExecutionIMStore();
        UUID taskId = UUID.randomUUID();
        TaskSchedule schedule = new TaskSchedule(taskId, Instant.EPOCH, true, 1);
        task(tasks, schedules, schedule, TaskStatus.PAUSE);
        executions.addTaskExecution(new TaskExecution(taskId, schedule.getTaskScheduleId(), Instant.EPOCH));

        new Executor(0, tasks, schedules, executions).addScheduledExecution(executions.claimDueExecutions().getFirst());

        assertThat(executions.getTaskExecutionsForTask(taskId)).singleElement()
                .satisfies(execution -> assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.DISCARDED));
        assertThat(executions.claimDueExecutions()).isEmpty();
    }

    private Task task(TaskStore tasks, TaskScheduleStore schedules, TaskSchedule schedule, TaskStatus status) {
        Task task = mock(Task.class);
        when(task.getTaskId()).thenReturn(schedule.getTaskID());
        when(task.getTaskScheduleId()).thenReturn(schedule.getTaskScheduleId());
        when(task.getTaskStatus()).thenReturn(status);
        when(tasks.getTask(schedule.getTaskID())).thenReturn(task);
        when(schedules.getTaskSchedule(schedule.getTaskScheduleId())).thenReturn(schedule);
        return task;
    }
}
