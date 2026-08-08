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

    public enum TaskStatus {
        ACTIVE, CANCEL, PAUSE, COMPLETED
    }

    public Task(String taskName, TaskStatus taskStatus) {
        this.taskId = UUID.randomUUID();
        this.taskName = taskName;
        this.taskStatus = taskStatus;
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

    public abstract void execute();
}