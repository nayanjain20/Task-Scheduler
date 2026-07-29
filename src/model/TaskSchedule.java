package model;

import java.time.Instant;

public class TaskSchedule {
    Task task;
    Instant startTime;
    Boolean isRecurring;
    Integer intervalSeconds;

    public TaskSchedule(Task task, Instant startTime, boolean isRecurring, Integer intervalSeconds) {
        this.task = task;
        this.startTime = startTime;
        this.isRecurring = isRecurring;
        this.intervalSeconds = intervalSeconds;
    }

    public Task getTask(){
        return task;
    }

    public Instant getStartTime(){
        return startTime;
    }

    public Boolean isRecurring(){
        return isRecurring;
    }

    public Integer getIntervalSeconds(){
        return intervalSeconds;
    }
}