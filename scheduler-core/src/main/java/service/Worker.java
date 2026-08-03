package service;

import java.util.Queue;

import model.ScheduledExecution;
import model.ScheduledExecution.ExecutionStatus;
import util.Logger;

/**
 * Worker thread that blocks on the shared execution queue.
 * Picks up tasks one at a time and calls task.execute().
 */
public class Worker implements Runnable {
    Queue<ScheduledExecution> executionQueue;
    int workerId;
    public static final int MAX_RETRY = 3;

    Worker(Queue<ScheduledExecution> executionQueue, int workerId) {
        this.executionQueue = executionQueue;
        this.workerId = workerId;
    }

    @Override
    public void run() {

        while (true) {
            ScheduledExecution execution = null;

            try {
                synchronized (executionQueue) {
                    while (executionQueue.isEmpty()) {
                        executionQueue.wait();
                    }
                    execution = executionQueue.poll();
                    execution.setWorker(this);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (execution != null) {

                int retry = MAX_RETRY;
                while (retry > 0) {
                    try {
                        execution.getTaskSchedule().getTask().execute();
                        break;
                    } catch (Exception e) {
                        retry -= 1;
                    }
                }
                if (retry == 0) {
                    execution.setExecutionStatus(ExecutionStatus.FAILED);
                    Logger.log("[WORKER-" + workerId + "] Executing: "
                            + execution.getTaskSchedule().getTask().getTaskName() + " Remaining tasks: "
                            + executionQueue.size());
                } else {
                    execution.setExecutionStatus(ExecutionStatus.COMPLETED);
                    Logger.log("[WORKER-" + workerId + "] Executing: "
                            + execution.getTaskSchedule().getTask().getTaskName() + " Remaining tasks: "
                            + executionQueue.size());
                }
            }
        }

    }

    public int getWorkerId() {
        return workerId;
    }

}
