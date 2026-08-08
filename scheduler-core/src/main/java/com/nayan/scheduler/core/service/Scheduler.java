package com.nayan.scheduler.core.service;

import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.util.Logger;

/**
 * Core scheduler that maintains a priority queue of upcoming executions.
 * Wakes up precisely when the next task is due using timed wait().
 * Thread-safe via synchronized methods and wait/notifyAll.
 */
public class Scheduler {

    Instant nextWakeup;
    PriorityQueue<TaskExecution> scheduledExecutions;
    Executor executor;
    TaskStore taskStore;
    TaskScheduleStore taskScheduleStore;
    TaskExecutionStore taskExecutionStore;

    public Scheduler(Executor executor, TaskStore taskStore, TaskScheduleStore taskScheduleStore,
            TaskExecutionStore taskExecutionStore) {
        this.taskStore = taskStore;
        this.taskScheduleStore = taskScheduleStore;
        this.taskExecutionStore = taskExecutionStore;
        nextWakeup = Instant.MAX;
        scheduledExecutions = new PriorityQueue<>((a, b) -> a.getExecutionTime().compareTo(b.getExecutionTime()));
        this.executor = executor;
    }

    public synchronized void addScheduledExecution(TaskExecution execution) {
        scheduledExecutions.add(execution);
        taskExecutionStore.addTaskExecution(execution);
        if (scheduledExecutions.size() > 0 && nextWakeup.isAfter(scheduledExecutions.peek().getExecutionTime())) {
            nextWakeup = scheduledExecutions.peek().getExecutionTime();
        }
        notifyAll();
        UUID taskId = execution.getTaskId();
        Task task = taskStore.getTask(taskId);
        Logger.log("[SCHEDULER] Added: " + task.getTaskName() + " | at: "
                + execution.getExecutionTime());
    }

    public synchronized void processScheduledExecutions() {
        Instant currentInstant = Instant.now();

        while (scheduledExecutions.size() > 0
                && scheduledExecutions.peek().getExecutionTime().compareTo(currentInstant) <= 0) {
            TaskExecution execution = scheduledExecutions.poll();
            UUID taskId = execution.getTaskId();
            Task task = taskStore.getTask(taskId);

            if (task.getTaskStatus().equals(TaskStatus.ACTIVE)) {
                executor.addScheduledExecution(execution);
                TaskExecution nextScheduledExecution = getNextScheduledExecution(execution);
                if (nextScheduledExecution != null) {
                    addScheduledExecution(nextScheduledExecution);
                } else {
                    task.setTaskStatus(TaskStatus.COMPLETED);
                    taskStore.updateTask(task);
                }
            } else {
                execution.setExecutionStatus(ExecutionStatus.SKIPPED);
                taskExecutionStore.updateTaskExecution(execution);
                Logger.log("[SKIPPING] task: " + task.getTaskId() + " - " + task.getTaskName() + " | status: "
                        + task.getTaskStatus());
            }
        }

    }

    public synchronized Instant getNextWakeup() {
        return nextWakeup;
    }

    public synchronized void waitUntilNextExecution() throws InterruptedException {
        if (scheduledExecutions.size() == 0) {
            wait();
        } else {
            long diff = Duration.between(Instant.now(), scheduledExecutions.peek().getExecutionTime()).toMillis();
            if (diff > 0) {
                wait(diff);
            }
        }
    }

    public TaskExecution getNextScheduledExecution(TaskExecution execution) {
        UUID taskScheduleId = execution.getTaskScheduleId();
        TaskSchedule taskSchedule = taskScheduleStore.getTaskSchedule(taskScheduleId);

        if (!taskSchedule.isRecurring()) {
            return null;
        }

        Instant executionTime = execution.getExecutionTime();

        Instant nextExecutionTime = executionTime.plusSeconds(taskSchedule.getIntervalSeconds());
        return new TaskExecution(execution.getTaskId(), taskScheduleId, nextExecutionTime);
    }

}
