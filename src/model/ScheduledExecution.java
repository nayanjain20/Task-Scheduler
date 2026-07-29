package model;

import java.time.Instant;

public class ScheduledExecution{
    TaskSchedule taskSchedule;
    Instant executionTime;

    public ScheduledExecution(TaskSchedule taskSchedule, Instant executionTime) {
        this.taskSchedule = taskSchedule;
        this.executionTime = executionTime;
    }

    public Instant getExecutionTime(){
        return executionTime;
    }

    public TaskSchedule gettTaskSchedule(){
        return taskSchedule;
    }

    public ScheduledExecution getNextScheduledExecution(){
        if(taskSchedule.getIntervalSeconds()==null){
            return null;
        }
        Instant nextExecutionTime = executionTime.plusSeconds(taskSchedule.intervalSeconds);
        return new ScheduledExecution(taskSchedule, nextExecutionTime);
    }
    
}