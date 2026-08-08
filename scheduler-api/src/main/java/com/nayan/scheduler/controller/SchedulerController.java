package com.nayan.scheduler.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.dto.CreateTaskRequest;
import com.nayan.scheduler.dto.CreateTaskResponse;
import com.nayan.scheduler.dto.GetAllTasksResponse;
import com.nayan.scheduler.service.SchedulerApiService;

@RestController
public class SchedulerController {

    private final SchedulerApiService schedulerApiService;

    SchedulerController(SchedulerApiService schedulerApiService) {
        this.schedulerApiService = schedulerApiService;
    }

    @GetMapping("/health")
    public String health() {
        return "Scheduler APi is running.";
    }

    @PostMapping("/tasks")
    public CreateTaskResponse createTask(@RequestBody CreateTaskRequest request) {
        UUID taskId = schedulerApiService.addNewTask(request);
        if (taskId != null) {
            return new CreateTaskResponse(taskId, "Complete");
        }
        return new CreateTaskResponse(null, "Complete");
    }

    @GetMapping("/tasks")
    public GetAllTasksResponse getTasks() {
        List<Task> tasks = schedulerApiService.getAllTasks();
        return new GetAllTasksResponse(tasks);
    }

    @GetMapping("/tasks/{taskId}/executions")
    public List<TaskExecution> getTaskExecutions(@PathVariable(value = "taskId") UUID taskId) {
        return schedulerApiService.getTaskExecutions(taskId);
    }
}