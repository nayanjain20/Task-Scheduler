package com.nayan.scheduler.core.model;

import java.time.Instant;

import com.nayan.scheduler.core.util.Logger;

public class PrintTask extends Task {

    public PrintTask(String taskName) {
        super(taskName, TaskStatus.ACTIVE);
    }

    void print() {
        Logger.log("[" + Instant.now() + "] " + taskName);
    }

    @Override
    public void execute() {
        print();
    }

}
