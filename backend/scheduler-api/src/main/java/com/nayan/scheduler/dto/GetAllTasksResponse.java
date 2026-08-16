package com.nayan.scheduler.dto;

import java.util.List;

import com.nayan.scheduler.core.model.Task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GetAllTasksResponse {
    private List<Task> tasks;
    private int count;

}
