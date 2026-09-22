package com.nayan.scheduler.core.engine;

import java.time.Instant;
import java.util.UUID;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;

public class TaskExecutionPlanner {
    private final TaskStore taskStore;
    private final TaskScheduleStore taskScheduleStore;

    public TaskExecutionPlanner(TaskStore taskStore, TaskScheduleStore taskScheduleStore) {
        this.taskStore = taskStore;
        this.taskScheduleStore = taskScheduleStore;
    }

    public TaskExecution createInitialTaskExecution(UUID taskId) {
        return createTaskExecution(taskId, false);
    }

    public TaskExecution createNextTaskExecution(UUID taskId) {
        return createTaskExecution(taskId, true);
    }

    private TaskExecution createTaskExecution(UUID taskId, boolean next) {
        Task task = taskStore.getTask(taskId);
        if (task == null) {
            throw new IllegalStateException("Task does not exist: " + taskId);
        }
        TaskSchedule schedule = taskScheduleStore.getTaskSchedule(task.getTaskScheduleId());
        if (schedule == null) {
            throw new IllegalStateException("Schedule does not exist for task: " + taskId);
        }
        if (next && !schedule.isRecurring()) {
            return null;
        }
        return new TaskExecution(taskId, task.getTaskScheduleId(),
                Instant.now().plusSeconds(schedule.getIntervalSeconds()));
    }
}
