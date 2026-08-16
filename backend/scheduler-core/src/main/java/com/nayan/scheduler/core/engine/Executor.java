package com.nayan.scheduler.core.engine;

import java.util.LinkedList;
import java.util.Queue;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.util.Logger;

/**
 * Dispatches due tasks to a fixed pool of Worker threads.
 * Uses a shared LinkedList queue with notify() to wake idle workers.
 */
public class Executor {

    private final Queue<TaskExecution> executionQueue;

    public Executor(int workerCount, TaskStore taskStore, TaskExecutionStore taskExecutionStore) {
        this.executionQueue = new LinkedList<>();
        for (int i = 0; i < workerCount; i++) {
            Worker worker = new Worker(executionQueue, i, taskStore, taskExecutionStore);
            Thread workerThread = new Thread(worker, "scheduler-worker-" + i);
            workerThread.setDaemon(true);
            workerThread.start();
        }
        Logger.log("[EXECUTOR] Started with worker count: " + workerCount);
    }

    public void addScheduledExecution(TaskExecution scheduledExecution) {
        synchronized (executionQueue) {
            executionQueue.add(scheduledExecution);
            executionQueue.notify();
        }
    }

}
