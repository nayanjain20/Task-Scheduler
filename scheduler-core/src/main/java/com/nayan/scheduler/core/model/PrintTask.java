package com.nayan.scheduler.core.model;

import java.time.Instant;
import java.util.UUID;

import com.nayan.scheduler.core.util.Logger;

public class PrintTask extends Task {

    public PrintTask(String taskName) {
        super(taskName, TaskStatus.ACTIVE, TaskType.PRINT);
    }

    public PrintTask(UUID taskId,
            String taskName,
            UUID taskScheduleId,
            TaskStatus status) {
        super(taskId, taskName, taskScheduleId, status, TaskType.PRINT);
    }

    void print() {
        Logger.log("[" + Instant.now() + "] " + taskName);
    }

    @Override
    public void execute() {
        print();
    }

}
