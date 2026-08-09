package com.nayan.scheduler.core.model;

import java.time.Instant;
import java.util.UUID;

public class TaskSchedule {
    UUID taskScheduleId;
    UUID taskId;
    Instant startTime;
    boolean recurring;
    int intervalSeconds;

    public TaskSchedule(UUID taskId, Instant startTime, boolean isRecurring, Integer intervalSeconds) {
        this.taskScheduleId = UUID.randomUUID();
        this.taskId = taskId;
        this.startTime = startTime;
        this.recurring = isRecurring;
        this.intervalSeconds = intervalSeconds;
    }

    public TaskSchedule(UUID taskScheduleId, UUID taskId, Instant startTime, boolean recurring,
            int intervalSeconds) {
        this.taskScheduleId = taskScheduleId;
        this.taskId = taskId;
        this.startTime = startTime;
        this.recurring = recurring;
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
        return recurring;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }
}