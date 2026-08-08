package com.nayan.scheduler.cli;

import com.nayan.scheduler.cli.store.TaskExecutionIMStore;
import com.nayan.scheduler.cli.store.TaskIMStore;
import com.nayan.scheduler.cli.store.TaskScheduleIMStore;
import com.nayan.scheduler.core.service.Executor;
import com.nayan.scheduler.core.service.Scheduler;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;

public class Main {

    static Scheduler scheduler;
    static Executor executor;

    public static void main(String args[]) {

        TaskStore taskStore = new TaskIMStore();
        TaskScheduleStore taskScheduleStore = new TaskScheduleIMStore();
        TaskExecutionStore taskExecutionStore = new TaskExecutionIMStore();
        executor = new Executor(5, taskStore, taskExecutionStore);
        scheduler = new Scheduler(executor, taskStore, taskScheduleStore, taskExecutionStore);
        Runnable clientRunnable = new Client(scheduler, taskStore, taskScheduleStore, taskExecutionStore);
        Runnable schedulerRunnable = new SchedulerProcess(scheduler);
        Thread clientThread = new Thread(clientRunnable);
        Thread schedulerThread = new Thread(schedulerRunnable);
        schedulerThread.setDaemon(true);
        clientThread.start();
        schedulerThread.start();
    }
}

class SchedulerProcess implements Runnable {
    Scheduler scheduler;

    SchedulerProcess(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        System.out.println("Scheduler started\n");
        while (true) {
            // System.out.println("In Loop");
            try {
                scheduler.waitUntilNextExecution();
                scheduler.processScheduledExecutions();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[SCHEDULER] Interrupted: " + e.getMessage());
            }
        }
    }
}
