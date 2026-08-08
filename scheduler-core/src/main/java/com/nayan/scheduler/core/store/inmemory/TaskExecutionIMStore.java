package com.nayan.scheduler.core.store.inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;

public class TaskExecutionIMStore implements TaskExecutionStore {

    Map<UUID, Map<UUID, TaskExecution>> taskToTaskExecutionsMap;

    public TaskExecutionIMStore() {
        taskToTaskExecutionsMap = new HashMap<>();
    }

    @Override
    public TaskExecution addTaskExecution(TaskExecution taskExecution) {
        taskToTaskExecutionsMap.computeIfAbsent(taskExecution.getTaskId(), k -> new HashMap<UUID, TaskExecution>())
                .put(taskExecution.getTaskExecutionId(), taskExecution);
        return taskExecution;
    }

    @Override
    public List<TaskExecution> getTaskExecutionsForTask(UUID taskId) {
        return new ArrayList<>(taskToTaskExecutionsMap.getOrDefault(taskId, Collections.emptyMap()).values());
    }

    @Override
    public boolean updateTaskExecution(TaskExecution taskExecution) {
        taskToTaskExecutionsMap.computeIfAbsent(taskExecution.getTaskId(), k -> new HashMap<UUID, TaskExecution>())
                .put(taskExecution.getTaskExecutionId(), taskExecution);
        return true;
    }

    @Override
    public boolean discardExecutionsForTask(UUID taskId) {
        List<TaskExecution> taskExecutions = getTaskExecutionsForTask(taskId);
        if (taskExecutions != null) {

            List<TaskExecution> pendingTaskExecutions = taskExecutions.stream()
                    .filter(exe -> exe.getExecutionStatus().equals(ExecutionStatus.PENDING)).toList();
            for (TaskExecution exe : pendingTaskExecutions) {
                exe.setExecutionStatus(ExecutionStatus.DISCARDED);
                updateTaskExecution(exe);
            }
            return true;
        }
        return false;
    }

}
