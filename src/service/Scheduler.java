package service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import model.ScheduledExecution;
import model.Task;
import model.ScheduledExecution.ExecutionStatus;
import model.Task.TaskStatus;
import util.Logger;

public class Scheduler {

    Instant nextWakeup;
    PriorityQueue<ScheduledExecution> scheduledExecutions;
    List<ScheduledExecution> executedList;
    Executor executor;


  
    public Scheduler(Executor executor ){
        nextWakeup = Instant.MAX;
        scheduledExecutions = new PriorityQueue<>((a,b)->a.getExecutionTime().compareTo(b.getExecutionTime()));
        executedList = new ArrayList<>();
        this.executor = executor; 
    }

    public synchronized void addScheduledExecution(ScheduledExecution scheduledExecution){
        scheduledExecutions.add(scheduledExecution);
        if(scheduledExecutions.size()>0 &&  nextWakeup.isAfter(scheduledExecutions.peek().getExecutionTime())){
            nextWakeup = scheduledExecutions.peek().getExecutionTime();        
        }
        notifyAll();
        Logger.log("[SCHEDULER] Added: " + scheduledExecution.getTaskSchedule().getTask().getTaskName() + " | at: " + scheduledExecution.getExecutionTime());
    }

    public synchronized void proceedScheduledExecution(){
        Instant currentInstant = Instant.now();

        while (scheduledExecutions.size()>0 && scheduledExecutions.peek().getExecutionTime().compareTo(currentInstant)<=0){
            ScheduledExecution execution = scheduledExecutions.poll();
            Task task = execution.getTaskSchedule().getTask();

            if(task.getTaskStatus().equals(TaskStatus.ACTIVE)){
                execution.setExecutionStatus(ExecutionStatus.IN_QUEUE);
                executor.addScheduledExecution(execution);    
                ScheduledExecution nextScheduledExecution = execution.getNextScheduledExecution();
                if(nextScheduledExecution!=null){
                    addScheduledExecution(nextScheduledExecution);
                    task.getScheduledExecutions().add(nextScheduledExecution);
                }else{
                    task.setTaskStatus(TaskStatus.COMPLETED);
                }
            }else{
                execution.setExecutionStatus(ExecutionStatus.SKIPPED);
                Logger.log("[SKIPPING] task: " + task.getTaskId() + " - " + task.getTaskName() + " | status: " + task.getTaskStatus());
                notifyAll();
                return;
            }             
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
