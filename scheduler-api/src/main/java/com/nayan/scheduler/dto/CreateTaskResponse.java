package com.nayan.scheduler.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor

public class CreateTaskResponse {
    private UUID taskId;
    private String status;
}
