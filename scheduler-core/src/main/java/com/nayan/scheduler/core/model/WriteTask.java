package com.nayan.scheduler.core.model;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class WriteTask extends Task {
    String filePath;
    String message;

    public WriteTask(String taskName, String filePath, String message) {
        super(taskName, TaskStatus.ACTIVE);
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
}