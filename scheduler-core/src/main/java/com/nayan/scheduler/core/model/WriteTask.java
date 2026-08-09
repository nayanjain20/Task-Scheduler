package com.nayan.scheduler.core.model;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

public class WriteTask extends Task {
    String filePath;
    String message;

    public WriteTask(String taskName, String filePath, String message) {
        super(taskName, TaskStatus.ACTIVE, TaskType.WRITE);
        this.filePath = filePath;
        this.message = message;
    }

    public WriteTask(UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus status,
            String filePath,
            String message) {

        super(taskId, taskName, taskScheduleId, status, TaskType.WRITE);
        this.filePath = filePath;
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void writeToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            writer.println(message);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void execute() {
        writeToFile();
    }

    public String getMessage() {
        return message;
    }
}