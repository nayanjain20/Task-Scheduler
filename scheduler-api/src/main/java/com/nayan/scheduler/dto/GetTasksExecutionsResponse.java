package com.nayan.scheduler.dto;

import java.util.List;

import com.nayan.scheduler.core.model.TaskExecution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GetTasksExecutionsResponse {
    private List<TaskExecution> executions;
    private int count;
}
