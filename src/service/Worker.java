package service;

import java.util.Queue;

import model.ScheduledExecution;
import model.ScheduledExecution.ExecutionStatus;
import util.Logger;

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
                    execution.setExecutionStatus(ExecutionStatus.COMPLETE);
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
