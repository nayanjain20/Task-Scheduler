package com.nayan.scheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nayan.scheduler.core.factory.TaskFactory;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.service.TaskSchedulerService;
import com.nayan.scheduler.dto.CreateTaskRequest;
import com.nayan.scheduler.dto.ScheduleRequest;
import com.nayan.scheduler.dto.TaskType;

@Service
public class SchedulerApiService {

    private final TaskSchedulerService taskSchedulerService;

    public SchedulerApiService(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    public UUID addNewTask(CreateTaskRequest createTaskRequest) {
        Task task = createTask(createTaskRequest);
        if (task == null) {
            return null;
        }
        TaskSchedule taskSchedule = createTaskSchedule(task.getTaskId(), createTaskRequest.getSchedule());
        if (taskSchedule == null) {
            return null;
        }
        task.setTaskScheduleId(taskSchedule.getTaskScheduleId());
        taskSchedulerService.createTaskAndSchedule(task, taskSchedule);
        return task.getTaskId();
    }

    public Task createTask(CreateTaskRequest createTaskRequest) {
        if (createTaskRequest == null) {
            return null;
        }
        String taskName = createTaskRequest.getTaskName();
        TaskType taskType = createTaskRequest.getType();
        if (taskType == TaskType.PRINT) {
            return TaskFactory.createPrintTask(taskName);
        }
        if (taskType == TaskType.WRITE) {
            String filePath = createTaskRequest.getPayload().getFilePath();
            String message = createTaskRequest.getPayload().getMessage();
            if (filePath == null || filePath.isEmpty()) {
                return null;
            }
            return TaskFactory.createWriteTask(taskName, filePath, message);
        }
        if (taskType == TaskType.DELETE) {
            String filePath = createTaskRequest.getPayload().getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                return null;
            }
            return TaskFactory.createDeleteTask(taskName, filePath);
        }
        return null;
    }

    public TaskSchedule createTaskSchedule(UUID taskId, ScheduleRequest scheduleRequest) {
        Instant startTime = scheduleRequest.getStartTime();
        if (startTime == null) {
            startTime = Instant.now();
        }
        return new TaskSchedule(taskId, startTime, scheduleRequest.isRecurring(),
                scheduleRequest.getInterval());
    }

    public List<Task> getAllTasks() {
        return taskSchedulerService.getAllTasks();
    }

    public List<TaskExecution> getTaskExecutions(UUID taskId) {
        return taskSchedulerService.getAllTaskExecutionsForTask(taskId);
    }
}
