package model;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base for all task types. Holds identity, status, schedule,
 * and execution history. Subclasses implement execute().
 */
public abstract class Task {
    final int taskId;
    final String taskName;
    TaskStatus taskStatus;
    TaskSchedule taskSchedule;
    List<ScheduledExecution> scheduledExecutions;
    public static final int MAX_RETRY = 3;

    public enum TaskStatus {ACTIVE,DEACTIVE,PAUSE, COMPLETED}

    public Task(int taskId, String taskName, TaskStatus taskStatus){
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskStatus = taskStatus;
        scheduledExecutions = new LinkedList<>();
    }

    public int getTaskId(){
        return taskId;
    }
    public String getTaskName(){
        return  taskName;
    }
    public TaskStatus getTaskStatus(){
        return taskStatus;
    }
    public void setTaskStatus(TaskStatus taskStatus){
        this.taskStatus = taskStatus;
    }
    public TaskSchedule getTaskSchedule(){
        return taskSchedule;
    }
    public void setTaskSchedule(TaskSchedule taskSchedule){
        this.taskSchedule = taskSchedule;
    }
    public List<ScheduledExecution> getScheduledExecutions(){
        return this.scheduledExecutions;
    }

    public abstract void execute();
}