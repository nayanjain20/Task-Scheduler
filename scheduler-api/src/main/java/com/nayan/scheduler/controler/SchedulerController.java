package com.nayan.scheduler.controler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nayan.scheduler.core.service.Scheduler;
import com.nayan.scheduler.dto.CreateTaskRequest;
import com.nayan.scheduler.dto.CreateTaskResponse;

@RestController
public class SchedulerController {

    private final Scheduler scheduler;

    SchedulerController(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping("/health")
    public String health() {
        return "Scheduler APi is running.";
    }

    @PostMapping("/tasks")
    public CreateTaskResponse createTask(@RequestBody CreateTaskRequest request) {
        System.out.println(request);
        return new CreateTaskResponse(-1, "Complete");
    }
}
