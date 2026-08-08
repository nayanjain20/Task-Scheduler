package com.nayan.scheduler.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskRequest {

    private TaskType type;
    private String taskName;
    private ScheduleRequest schedule;
    private TaskPayload payload;
}
