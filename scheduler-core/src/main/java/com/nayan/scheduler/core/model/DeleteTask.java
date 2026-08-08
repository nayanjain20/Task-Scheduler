package com.nayan.scheduler.core.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.nayan.scheduler.core.util.Logger;

public class DeleteTask extends Task {

    String filePath;

    public DeleteTask(String taskName, String filePath) {
        super(taskName, TaskStatus.ACTIVE);
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
        // TODO Auto-generated method stub
        deleteFile();
    }

}
