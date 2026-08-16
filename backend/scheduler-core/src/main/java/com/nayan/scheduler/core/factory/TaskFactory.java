package com.nayan.scheduler.core.factory;

import com.nayan.scheduler.core.model.DeleteTask;
import com.nayan.scheduler.core.model.PrintTask;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.WriteTask;

import java.util.UUID;

import com.nayan.scheduler.core.model.Task.TaskStatus;

public class TaskFactory {
    public static Task createPrintTask(String taskName) {
        return new PrintTask(taskName);
    }

    public static Task createWriteTask(String taskName, String filePath, String message) {
        return new WriteTask(taskName, filePath, message);
    }

    public static Task createDeleteTask(String taskName, String filePath) {
        return new DeleteTask(taskName, filePath);
    }

    public static Task loadPrintTask(
            UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus taskStatus) {

        return new PrintTask(
                taskId,
                taskName,
                taskScheduleId,
                taskStatus);
    }

    public static Task loadWriteTask(
            UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus taskStatus,
            String filePath,
            String message) {

        return new WriteTask(
                taskId,
                taskName,
                taskScheduleId,
                taskStatus,
                filePath,
                message);
    }

    public static Task loadDeleteTask(
            UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus taskStatus,
            String filePath) {

        return new DeleteTask(
                taskId,
                taskName,
                taskScheduleId,
                taskStatus,
                filePath);
    }
}