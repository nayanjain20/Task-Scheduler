package com.nayan.scheduler.core.engine;

import java.util.LinkedList;
import java.util.Queue;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.util.Logger;

/**
 * Dispatches due tasks to a fixed pool of Worker threads.
 * Uses a shared LinkedList queue with notify() to wake idle workers.
 */
public class Executor {

    private final Queue<TaskExecution> executionQueue;
    private final TaskStore taskStore;
    private final TaskExecutionStore taskExecutionStore;
    private final TaskExecutionPlanner planner;

    public Executor(int workerCount, TaskStore taskStore, TaskScheduleStore taskScheduleStore,
            TaskExecutionStore taskExecutionStore) {
        this.taskStore = taskStore;
        this.taskExecutionStore = taskExecutionStore;
        this.planner = new TaskExecutionPlanner(taskStore, taskScheduleStore);
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
        Task task = taskStore.getTask(scheduledExecution.getTaskId());
        if (task == null) {
            throw new IllegalStateException("Task does not exist: " + scheduledExecution.getTaskId());
        }
        if (task.getTaskStatus() != TaskStatus.ACTIVE) {
            taskExecutionStore.discardExecutionsForTask(task.getTaskId());
            Logger.log("[EXECUTOR] Not dispatching task: " + task.getTaskId() + " | status: " + task.getTaskStatus());
            return;
        }
        TaskExecution next = planner.createNextTaskExecution(task.getTaskId());
        if (next != null) {
            taskExecutionStore.addTaskExecution(next);
        } else {
            task.setTaskStatus(TaskStatus.COMPLETED);
            taskStore.updateTask(task);
        }
        synchronized (executionQueue) {
            executionQueue.add(scheduledExecution);
            executionQueue.notify();
        }
    }

}
