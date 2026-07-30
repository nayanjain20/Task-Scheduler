package service;

import java.util.LinkedList;
import java.util.Queue;

import model.ScheduledExecution;

public class Executor{

    Queue<ScheduledExecution> executionQueue;
    int WORKER_COUNT = 10;

    public Executor(){
        executionQueue = new LinkedList<>();
        for(int i=0; i<WORKER_COUNT;i++){
            Worker worker= new Worker(executionQueue,i);
            Thread workerThread = new Thread(worker);
            workerThread.start();
        }
        System.out.println("[EXECUTOR] Started with worker count: " + WORKER_COUNT);
    }

    public void addScheduledExecution(ScheduledExecution scheduledExecution){
        synchronized(executionQueue){
            executionQueue.add(scheduledExecution);
            executionQueue.notify();
        }
    }
    
}
