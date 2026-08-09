package com.nayan.scheduler.core.model;

import java.util.UUID;

/**
 * Abstract base for all task types. Holds identity, status, schedule,
 * and execution history. Subclasses implement execute().
 */
public abstract class Task {
    final UUID taskId;
    final String taskName;
    TaskStatus taskStatus;
    UUID taskScheduleId;
    TaskType taskType;

    public enum TaskStatus {
        ACTIVE, CANCEL, PAUSE, COMPLETED
    }

    public enum TaskType {
        PRINT,
        WRITE,
        DELETE
    }

    public Task(String taskName, TaskStatus taskStatus, TaskType taskType) {
        this.taskId = UUID.randomUUID();
        this.taskName = taskName;
        this.taskStatus = taskStatus;
        this.taskType = taskType;
    }

    public Task(UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus taskStatus,
            TaskType taskType) {

        this.taskId = taskId;
        this.taskName = taskName;
        this.taskScheduleId = taskScheduleId;
        this.taskStatus = taskStatus;
        this.taskType = taskType;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public void setTaskScheduleId(UUID taskScheduleId) {
        this.taskScheduleId = taskScheduleId;
    }

    public UUID getTaskScheduleId() {
        return taskScheduleId;
    }

    public TaskType getTaskType() {
        return this.taskType;
    }

    public abstract void execute();
}