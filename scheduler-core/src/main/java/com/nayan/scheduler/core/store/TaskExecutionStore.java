package com.nayan.scheduler.core.store;

import java.util.List;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskExecution;

public interface TaskExecutionStore {
    public TaskExecution addTaskExecution(TaskExecution taskExecution);

    public List<TaskExecution> getTaskExecutionsForTask(UUID taskId);

    public boolean updateTaskExecution(TaskExecution taskExecution);

    public boolean discardExecutionsForTask(UUID taskId);

}
