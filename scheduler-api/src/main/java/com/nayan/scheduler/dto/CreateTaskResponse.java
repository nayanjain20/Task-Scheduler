package com.nayan.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor

public class CreateTaskResponse {
    private int taskId;
    private String status;
}
