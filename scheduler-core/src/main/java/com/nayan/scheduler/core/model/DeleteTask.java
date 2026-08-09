package com.nayan.scheduler.core.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.nayan.scheduler.core.util.Logger;

public class DeleteTask extends Task {

    String filePath;

    public DeleteTask(String taskName, String filePath) {
        super(taskName, TaskStatus.ACTIVE, TaskType.DELETE);
        this.filePath = filePath;
    }

    public DeleteTask(UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus taskStatus,
            String filePath) {
        super(taskId, taskName, taskScheduleId, taskStatus, TaskType.DELETE);
        this.filePath = filePath;
    }

    public void deleteFile() {
        try {
            boolean delete = Files.deleteIfExists(Path.of(filePath));
            if (delete) {
                Logger.log("[CleanupTask] Deleted: " + filePath);
            } else {
                Logger.log("[CleanupTask] File not found: " + filePath);
            }
        } catch (IOException e) {
            Logger.log("[CleanupTask] Failed to delete: " + filePath);
        }
    }

    @Override
    public void execute() {
        deleteFile();
    }

    public String getFilePath() {
        return filePath;
    }

}
