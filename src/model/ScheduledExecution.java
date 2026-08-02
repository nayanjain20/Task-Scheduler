package model;

import java.time.Instant;

import service.Worker;

public class ScheduledExecution{
    TaskSchedule taskSchedule;
    Instant executionTime;
    Worker worker;
    ExecutionStatus executionStatus;
    public enum ExecutionStatus {COMPLETE,SKIPPED,PENDING, IN_QUEUE};

    public ScheduledExecution(TaskSchedule taskSchedule, Instant executionTime) {
        this.taskSchedule = taskSchedule;
        this.executionTime = executionTime;
        this.executionStatus = ExecutionStatus.PENDING;
    }

    public Instant getExecutionTime(){
        return executionTime;
    }

    public TaskSchedule getTaskSchedule(){
        return taskSchedule;
    }

    public ScheduledExecution getNextScheduledExecution(){
        if(!taskSchedule.isRecurring()){
            return null;
        }
        Instant nextExecutionTime = executionTime.plusSeconds(taskSchedule.intervalSeconds);
        return new ScheduledExecution(taskSchedule, nextExecutionTime);
    }
    public Worker getWorker(){
        return worker;
    }
    public void setWorker(Worker  worker){
        this.worker = worker;
    }
    public ExecutionStatus getExecutionStatus(){
        return executionStatus;
    }
    public void setExecutionStatus(ExecutionStatus executionStatus){
        this.executionStatus = executionStatus;
    }
    
}