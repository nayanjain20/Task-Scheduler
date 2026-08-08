package com.nayan.scheduler.dto;

public class CreateTaskRequest {

    private TaskType type;
    private String taskName;
    private ScheduleRequest schedule;
    private TaskPayload payload;
}
