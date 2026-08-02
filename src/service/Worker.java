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
    
    Worker(Queue<ScheduledExecution> executionQueue, int workerId){
        this.executionQueue = executionQueue;
        this.workerId = workerId;
    }
    

    @Override
    public void run() {

        while(true){
            ScheduledExecution execution;
        
            try {
                synchronized(executionQueue){
                    while(executionQueue.isEmpty()){
                        executionQueue.wait();
                    }
                    execution = executionQueue.poll();
                    execution.setWorker(this);        
                    execution.getTaskSchedule().getTask().execute();
                    execution.setExecutionStatus(ExecutionStatus.COMPLETED);
                    Logger.log("[WORKER-" + workerId + "] Executing: " + execution.getTaskSchedule().getTask().getTaskName() + " Remaining tasks: " + executionQueue.size());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }
    public int getWorkerId(){
        return workerId;
    }
    
}
