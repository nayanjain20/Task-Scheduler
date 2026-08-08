package com.nayan.scheduler.core.service;

import java.util.List;
import java.util.UUID;

import com.nayan.scheduler.core.engine.Executor;
import com.nayan.scheduler.core.engine.Scheduler;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;

public class TaskSchedulerService {
    private final TaskStore taskStore;
    private final TaskScheduleStore taskScheduleStore;
    private final TaskExecutionStore taskExecutionStore;
    private final Scheduler scheduler;
    private final Executor executor;
    private final Thread schedulerThread;

    public TaskSchedulerService(TaskStore taskStore, TaskScheduleStore taskScheduleStore,
            TaskExecutionStore taskExecutionStore) {
        this.taskStore = taskStore;
        this.taskScheduleStore = taskScheduleStore;
        this.taskExecutionStore = taskExecutionStore;
        this.executor = new Executor(5, taskStore, taskExecutionStore);
        this.scheduler = new Scheduler(executor, taskStore, taskScheduleStore, taskExecutionStore);
        Runnable schedulerRunnable = new SchedulerProcess(scheduler);
        this.schedulerThread = new Thread(schedulerRunnable);
    }

    public void startScheduler() {
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    public boolean createTaskAndSchedule(Task task, TaskSchedule taskSchedule) {
        if (task == null || taskSchedule == null) {
            return false;
        }

        taskStore.addTask(task);
        taskScheduleStore.addTaskSchedule(taskSchedule);
        TaskExecution taskExecution = scheduler.createInitialTaskExecution(task.getTaskId());
        scheduler.addScheduledExecution(taskExecution);
        return true;
    }

    public boolean cancelTask(UUID taskId) {
        return updateTaskStatusAndDiscardExecutions(taskId, TaskStatus.CANCEL);
    }

    public boolean pauseTask(UUID taskId) {
        return updateTaskStatusAndDiscardExecutions(taskId, TaskStatus.PAUSE);
    }

    public boolean updateTaskStatusAndDiscardExecutions(UUID taskId, TaskStatus taskStatus) {
        Task task = taskStore.getTask(taskId);
        if (task != null) {
            task.setTaskStatus(taskStatus);
            taskExecutionStore.discardExecutionsForTask(taskId);
            return true;
        }
        return false;
    }

    public boolean resumeTask(UUID taskId) {
        Task task = taskStore.getTask(taskId);
        if (task == null || !task.getTaskStatus().equals(TaskStatus.PAUSE)) {
            return false;
        }
        TaskSchedule taskSchedule = taskScheduleStore.getTaskSchedule(task.getTaskScheduleId());
        if (taskSchedule == null) {
            return false;
        }

        TaskExecution taskExecution = scheduler.createInitialTaskExecution(task.getTaskId());
        scheduler.addScheduledExecution(taskExecution);
        return true;

    }

    public List<Task> getAllTasks() {
        return taskStore.getAllTasks();
    }

    public List<Task> getAllActiveTasks() {
        return taskStore.getAllActiveTasks();
    }

    public List<TaskExecution> getAllTaskExecutionsForTask(UUID taskId) {
        return taskExecutionStore.getTaskExecutionsForTask(taskId);
    }
}

class SchedulerProcess implements Runnable {
    Scheduler scheduler;

    SchedulerProcess(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        System.out.println("Scheduler started\n");
        while (true) {
            // System.out.println("In Loop");
            try {
                scheduler.waitUntilNextExecution();
                scheduler.processScheduledExecutions();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[SCHEDULER] Interrupted: " + e.getMessage());
            }
        }
    }
}
