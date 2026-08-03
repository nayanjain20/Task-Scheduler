package service;

import java.util.LinkedList;
import java.util.Queue;

import model.ScheduledExecution;
import util.Logger;

/**
 * Dispatches due tasks to a fixed pool of Worker threads.
 * Uses a shared LinkedList queue with notify() to wake idle workers.
 */
public class Executor{

    Queue<ScheduledExecution> executionQueue;
    int WORKER_COUNT = 2;

  
    public Executor(){
        executionQueue = new LinkedList<>();
        for(int i=0; i<WORKER_COUNT;i++){
            Worker worker= new Worker(executionQueue,i);
            Thread workerThread = new Thread(worker);
            workerThread.start();
        }
        Logger.log("[EXECUTOR] Started with worker count: " + WORKER_COUNT );
    }

    public void addScheduledExecution(ScheduledExecution scheduledExecution){
        synchronized(executionQueue){
            executionQueue.add(scheduledExecution);
            executionQueue.notify();
        }
    }
    
}
