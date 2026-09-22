package com.nayan.scheduler.core.engine;

import java.time.Instant;
import java.util.Queue;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.util.Logger;

/**
 * Worker thread that blocks on the shared execution queue.
 * Picks up tasks one at a time and calls task.execute().
 */
public class Worker implements Runnable {
    Queue<TaskExecution> executionQueue;
    int workerId;
    public static final int MAX_RETRY = 3;

    TaskStore taskStore;
    TaskExecutionStore taskExecutionStore;

    Worker(Queue<TaskExecution> executionQueue, int workerId, TaskStore taskStore,
            TaskExecutionStore taskExecutionStore) {
        this.executionQueue = executionQueue;
        this.workerId = workerId;
        this.taskStore = taskStore;
        this.taskExecutionStore = taskExecutionStore;
    }

    @Override
    public void run() {

        while (true) {
            TaskExecution execution = null;

            try {
                synchronized (executionQueue) {
                    while (executionQueue.isEmpty()) {
                        executionQueue.wait();
                    }
                    execution = executionQueue.poll();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (execution != null) {
                try {
                    taskExecutionStore.assignTaskExecutionToWorker(execution.getTaskExecutionId(), workerId);
                } catch (RuntimeException e) {
                    Logger.log("[WORKER-" + workerId + "] Could not claim execution "
                            + execution.getTaskExecutionId() + ": " + e);
                    continue;
                }
                execution.setWorkerId(workerId);
                execution.setExecutionStatus(ExecutionStatus.ASSIGNED);
                execution.setUpdatedAt(Instant.now());

                int retry = MAX_RETRY;
                Task task = taskStore.getTask(execution.getTaskId());
                while (retry > 0) {
                    try {
                        task.execute();
                        break;
                    } catch (Exception e) {
                        retry -= 1;
                        Logger.log("[WORKER-" + workerId + "] Execution failed: " + execution.getTaskExecutionId()
                                + " | attempts remaining: " + retry + " | " + e);
                    }
                }
                if (retry == 0) {
                    execution.setExecutionStatus(ExecutionStatus.FAILED);
                    taskExecutionStore.updateTaskExecution(execution);
                    Logger.log("[WORKER-" + workerId + "] Executing: "
                            + task.getTaskName() + " Remaining tasks: "
                            + executionQueue.size());
                } else {
                    execution.setExecutionStatus(ExecutionStatus.COMPLETED);
                    taskExecutionStore.updateTaskExecution(execution);
                    Logger.log("[WORKER-" + workerId + "] Executing: "
                            + task.getTaskName() + " Remaining tasks: "
                            + executionQueue.size());
                }
            }
        }

    }

    public int getWorkerId() {
        return workerId;
    }

}
