package com.nayan.scheduler.core.service;

import java.util.List;
import java.util.UUID;

import com.nayan.scheduler.core.engine.Executor;
import com.nayan.scheduler.core.engine.Scheduler;
import com.nayan.scheduler.core.engine.TaskExecutionPlanner;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.util.Logger;

public class TaskSchedulerService {
    private final TaskStore taskStore;
    private final TaskScheduleStore taskScheduleStore;
    private final TaskExecutionStore taskExecutionStore;
    private final Scheduler scheduler;
    private final Executor executor;
    private final Thread schedulerThread;
    private final TaskExecutionPlanner planner;

    public TaskSchedulerService(TaskStore taskStore, TaskScheduleStore taskScheduleStore,
            TaskExecutionStore taskExecutionStore, int workerCount) {
        this.taskStore = taskStore;
        this.taskScheduleStore = taskScheduleStore;
        this.taskExecutionStore = taskExecutionStore;
        this.planner = new TaskExecutionPlanner(taskStore, taskScheduleStore);
        this.executor = new Executor(workerCount, taskStore, taskScheduleStore, taskExecutionStore);
        this.scheduler = new Scheduler(executor, taskExecutionStore);
        Runnable schedulerRunnable = new SchedulerProcess(scheduler);
        this.schedulerThread = new Thread(schedulerRunnable);
    }

    public synchronized void startScheduler() {
        if (schedulerThread.getState() != Thread.State.NEW) {
            throw new IllegalStateException("Scheduler has already been started.");
        }
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    public boolean createTaskAndSchedule(Task task, TaskSchedule taskSchedule) {
        if (task == null || taskSchedule == null) {
            return false;
        }

        taskStore.addTask(task);
        taskScheduleStore.addTaskSchedule(taskSchedule);
        TaskExecution taskExecution = planner.createInitialTaskExecution(task.getTaskId());
        taskExecutionStore.addTaskExecution(taskExecution);
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
            taskStore.updateTask(task);
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
        task.setTaskStatus(TaskStatus.ACTIVE);
        taskStore.updateTask(task);

        TaskExecution taskExecution = planner.createInitialTaskExecution(task.getTaskId());
        taskExecutionStore.addTaskExecution(taskExecution);
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
    static final long POLL_INTERVAL_MILLIS = 1000;
    private final Scheduler scheduler;

    SchedulerProcess(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        Logger.log("[SCHEDULER] Polling execution store every second");
        while (!Thread.currentThread().isInterrupted()) {
            long started = System.nanoTime();
            try {
                scheduler.processScheduledExecutions();
            } catch (RuntimeException e) {
                Logger.log("[SCHEDULER] Poll failed: " + e);
            }
            try {
                long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
                Thread.sleep(Math.max(1, POLL_INTERVAL_MILLIS - elapsedMillis));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
