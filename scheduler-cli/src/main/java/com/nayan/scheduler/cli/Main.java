package com.nayan.scheduler.cli;

import com.nayan.scheduler.core.engine.Executor;
import com.nayan.scheduler.core.engine.Scheduler;
import com.nayan.scheduler.core.service.TaskSchedulerService;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;
import com.nayan.scheduler.core.store.inmemory.TaskIMStore;
import com.nayan.scheduler.core.store.inmemory.TaskScheduleIMStore;

public class Main {

    public static void main(String args[]) {

        TaskStore taskStore = new TaskIMStore();
        TaskScheduleStore taskScheduleStore = new TaskScheduleIMStore();
        TaskExecutionStore taskExecutionStore = new TaskExecutionIMStore();
        TaskSchedulerService taskSchedulerService = new TaskSchedulerService(taskStore, taskScheduleStore,
                taskExecutionStore);
        taskSchedulerService.startScheduler();
        Client client = new Client(taskSchedulerService);
        client.run();
    }
}
