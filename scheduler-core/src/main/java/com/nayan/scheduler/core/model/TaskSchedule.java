package com.nayan.scheduler.core.model;

import java.time.Instant;
import java.util.UUID;

public class TaskSchedule {
    UUID taskScheduleId;
    UUID taskId;
    Instant startTime;
    Boolean isRecurring;
    Integer intervalSeconds;

    public TaskSchedule(UUID taskId, Instant startTime, boolean isRecurring, Integer intervalSeconds) {
        this.taskScheduleId = UUID.randomUUID();
        this.taskId = taskId;
        this.startTime = startTime;
        this.isRecurring = isRecurring;
        this.intervalSeconds = intervalSeconds;
    }

    public UUID getTaskID() {
        return taskId;
    }

    public UUID getTaskScheduleId() {
        return taskScheduleId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Boolean isRecurring() {
        return isRecurring;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }
}