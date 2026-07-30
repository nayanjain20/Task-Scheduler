package service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import model.ScheduledExecution;

public class Scheduler {

    Instant nextWakeup;
    PriorityQueue<ScheduledExecution> scheduledExecutions;
    List<ScheduledExecution> executedList;
    Executor executor;

    public Scheduler(Executor executor
    ){
        nextWakeup = Instant.MAX;
        scheduledExecutions = new PriorityQueue<>((a,b)->a.getExecutionTime().compareTo(b.getExecutionTime()));
        executedList = new ArrayList<>();
        this.executor = executor; 
    }

    public synchronized void addScheduledExecution(ScheduledExecution scheduledExecution){
        scheduledExecutions.add(scheduledExecution);
        // scheduledExecution.gettTaskSchedule().getTask().execute();
        if(scheduledExecutions.size()>0 &&  nextWakeup.isAfter(scheduledExecutions.peek().getExecutionTime())){
            nextWakeup = scheduledExecutions.peek().getExecutionTime();        
        }
        notifyAll();
        System.out.println("[SCHEDULER] Added: " + scheduledExecution.gettTaskSchedule().getTask().getClass().getSimpleName() + " | at: " + scheduledExecution.getExecutionTime());
    }

    public synchronized void proceedScheduedExecution(){
        Instant currentInstant = Instant.now();
        System.out.println("[SCHEDULER] Proceeding executions at: " + currentInstant);

        while (scheduledExecutions.size()>0 && scheduledExecutions.peek().getExecutionTime().compareTo(currentInstant)<=0){
            ScheduledExecution execution = scheduledExecutions.poll();
            
            
            // executedList.add(execution);
            executor.addScheduledExecution(execution);
                  
            ScheduledExecution nextScheduledExecution = execution.getNextScheduledExecution();
            if(nextScheduledExecution!=null){
                addScheduledExecution(nextScheduledExecution);
            }
        }
        
        if(scheduledExecutions.size()>0){
            nextWakeup = scheduledExecutions.peek().getExecutionTime();        
        }else{
            nextWakeup = Instant.MAX;
        }
        
    }

    public synchronized Instant getNextWakeup(){
        return nextWakeup;
    }

    public synchronized void waitUntilNextExecution()throws InterruptedException {
        if(scheduledExecutions.size()==0){
            wait();
        }else{
            long diff = Duration.between(Instant.now(), scheduledExecutions.peek().getExecutionTime()).toMillis();
            if(diff>0){
                wait(diff);
            }
        }
    }
    
}
