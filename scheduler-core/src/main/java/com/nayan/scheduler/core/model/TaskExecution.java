package com.nayan.scheduler.core.model;

import java.time.Instant;
import java.util.UUID;

import com.nayan.scheduler.core.engine.Worker;

/**
 * Represents a single scheduled run of a task at a specific time.
 * Tracks its execution status and which worker handled it.
 */
public class TaskExecution {
    UUID taskExecutionId;
    UUID taskId;
    UUID taskScheduleId;
    Instant executionTime;
    Worker worker;
    ExecutionStatus executionStatus;

    public enum ExecutionStatus {
        COMPLETED, SKIPPED, PENDING, DISCARDED, FAILED
    };

    public TaskExecution(UUID taskId, UUID taskScheduleId, Instant executionTime) {
        this.taskExecutionId = UUID.randomUUID();
        this.taskScheduleId = taskScheduleId;
        this.taskId = taskId;
        this.executionTime = executionTime;
        this.executionStatus = ExecutionStatus.PENDING;
    }

    public UUID getTaskExecutionId() {
        return taskExecutionId;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getTaskScheduleId() {
        return taskScheduleId;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

}