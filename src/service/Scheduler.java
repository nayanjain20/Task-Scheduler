package service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import model.ScheduledExecution;

public class Scheduler implements Runnable{

    Instant nextWakeup;
    PriorityQueue<ScheduledExecution> scheduledExecutions;
    List<ScheduledExecution> executedList;

    public Scheduler(){
        nextWakeup = Instant.MAX;
        scheduledExecutions = new PriorityQueue<>((a,b)->a.getExecutionTime().compareTo(b.getExecutionTime()));
        executedList = new ArrayList<>();
    }

    public void addScheduledExecution(ScheduledExecution scheduledExecution, Thread schedulerThread){
        scheduledExecutions.add(scheduledExecution);
        schedulerThread.notify();

    }

    void sleep(long milliseconds){
        try{
            Thread.sleep(milliseconds);
        }catch (InterruptedException e){
            System.out.println("Cant sleep "+ e);
        }
    }

    @Override
    public void run(){
        // Instant currentInstant = Instant.now();

        // System.out.println("[" + currentInstant + "] Proceeding scheduled executions");

        while (true){
            
            if(scheduledExecutions.size()>0 && scheduledExecutions.peek().getExecutionTime().compareTo(Instant.now())<=0){
                long milliseconds = Duration.between(Instant.now(), scheduledExecutions.peek().getExecutionTime()).toMillis();
                sleep(milliseconds);
            }else if(scheduledExecutions.size() == 0){
                sleep(Long.MAX_VALUE);
            }else{

            }

            ScheduledExecution execution = scheduledExecutions.poll(); 
            
            // executedList.add(execution);
            execution.gettTaskSchedule().getTask().execute();
            
            
            ScheduledExecution nextScheduledExecution = execution.getNextScheduledExecution();
            if(nextScheduledExecution!=null){
                scheduledExecutions.add(nextScheduledExecution);
            }
        }
        
    }

    public Instant getNextWakeup(){
        return nextWakeup;
    }
    
}
