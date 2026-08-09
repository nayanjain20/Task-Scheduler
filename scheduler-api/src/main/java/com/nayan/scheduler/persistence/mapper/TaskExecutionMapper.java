package com.nayan.scheduler.persistence.mapper;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.persistence.entity.TaskExecutionEntity;

public final class TaskExecutionMapper {

    private TaskExecutionMapper() {
    }

    public static TaskExecutionEntity toEntity(TaskExecution execution) {
        return new TaskExecutionEntity(
                execution.getTaskExecutionId(),
                execution.getTaskId(),
                execution.getTaskScheduleId(),
                execution.getExecutionTime(),
                execution.getWorkerId(),
                execution.getExecutionStatus());
    }

    public static TaskExecution toModel(TaskExecutionEntity entity) {
        int workerId = entity.getWorkerId() == null ? -1 : entity.getWorkerId();
        return new TaskExecution(
                entity.getTaskExecutionId(),
                entity.getTaskId(),
                entity.getTaskScheduleId(),
                entity.getExecutionTime(),
                workerId,
                entity.getExecutionStatus());
    }
}