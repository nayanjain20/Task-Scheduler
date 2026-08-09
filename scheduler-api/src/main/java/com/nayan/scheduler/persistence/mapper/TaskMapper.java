package com.nayan.scheduler.persistence.mapper;

import com.nayan.scheduler.core.factory.TaskFactory;
import com.nayan.scheduler.core.model.DeleteTask;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.WriteTask;
import com.nayan.scheduler.persistence.entity.TaskEntity;

public class TaskMapper {
    public static TaskEntity toEntity(Task task) {

        String filePath = null;
        String message = null;

        if (task instanceof WriteTask writeTask) {
            filePath = writeTask.getFilePath();
            message = writeTask.getMessage();
        } else if (task instanceof DeleteTask deleteTask) {
            filePath = deleteTask.getFilePath();
        }

        return new TaskEntity(
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskScheduleId(),
                task.getTaskStatus(),
                task.getTaskType(),
                message,
                filePath);
    }

    public static Task toModel(TaskEntity entity) {

        switch (entity.getTaskType()) {

            case PRINT:
                return TaskFactory.loadPrintTask(
                        entity.getTaskId(),
                        entity.getTaskName(),
                        entity.getTaskScheduleId(),
                        entity.getTaskStatus());

            case WRITE:
                return TaskFactory.loadWriteTask(
                        entity.getTaskId(),
                        entity.getTaskName(),
                        entity.getTaskScheduleId(),
                        entity.getTaskStatus(),
                        entity.getFilePath(),
                        entity.getMessage());

            case DELETE:
                return TaskFactory.loadDeleteTask(
                        entity.getTaskId(),
                        entity.getTaskName(),
                        entity.getTaskScheduleId(),
                        entity.getTaskStatus(),
                        entity.getFilePath());

            default:
                throw new IllegalArgumentException("Unsupported task type");
        }
    }

}
