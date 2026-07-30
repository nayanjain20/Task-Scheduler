package service;

import java.util.Queue;

import model.ScheduledExecution;

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
                    System.out.println("[WORKER-" + workerId + "] Executing: " + execution.gettTaskSchedule().getTask().getClass().getSimpleName());
                }
                execution.gettTaskSchedule().getTask().execute();     
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }
    
}
