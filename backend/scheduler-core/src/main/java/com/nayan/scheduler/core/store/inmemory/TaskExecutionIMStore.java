package com.nayan.scheduler.core.store.inmemory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;

public class TaskExecutionIMStore implements TaskExecutionStore {

    private final Map<UUID, TaskExecution> executions = new HashMap<>();
    private final PriorityQueue<TaskExecution> pendingExecutions = new PriorityQueue<>(
            Comparator.comparing(TaskExecution::getExecutionTime).thenComparing(TaskExecution::getTaskExecutionId));

    @Override
    public synchronized List<TaskExecution> claimDueExecutions() {
        Instant now = Instant.now();
        List<TaskExecution> claimed = new ArrayList<>();
        while (claimed.size() < CLAIM_BATCH_SIZE && !pendingExecutions.isEmpty()
                && pendingExecutions.peek().getExecutionTime().isBefore(now)) {
            TaskExecution execution = pendingExecutions.poll();
            execution.setExecutionStatus(ExecutionStatus.IN_QUEUE);
            execution.setWorkerId(-1);
            execution.setUpdatedAt(now);
            claimed.add(snapshot(execution));
        }
        return claimed;
    }

    @Override
    public synchronized List<TaskExecution> getAllTaskExecutionsOfStatus(ExecutionStatus status) {
        return executions.values().stream().filter(execution -> execution.getExecutionStatus() == status)
                .map(this::snapshot).toList();
    }

    @Override
    public synchronized TaskExecution addTaskExecution(TaskExecution taskExecution) {
        save(taskExecution);
        return snapshot(executions.get(taskExecution.getTaskExecutionId()));
    }

    @Override
    public synchronized List<TaskExecution> getTaskExecutionsForTask(UUID taskId) {
        return executions.values().stream().filter(execution -> execution.getTaskId().equals(taskId))
                .map(this::snapshot).toList();
    }

    @Override
    public synchronized boolean updateTaskExecution(TaskExecution taskExecution) {
        save(taskExecution);
        return true;
    }

    private void save(TaskExecution taskExecution) {
        taskExecution.setUpdatedAt(Instant.now());
        TaskExecution stored = snapshot(taskExecution);
        TaskExecution previous = executions.put(stored.getTaskExecutionId(), stored);
        if (previous != null && previous.getExecutionStatus() == ExecutionStatus.PENDING) {
            pendingExecutions.remove(previous);
        }
        if (stored.getExecutionStatus() == ExecutionStatus.PENDING) {
            pendingExecutions.add(stored);
        }
    }

    @Override
    public synchronized void assignTaskExecutionToWorker(UUID taskExecutionId, int workerId) {
        if (taskExecutionId == null || workerId < 0) {
            throw new IllegalArgumentException("Execution ID is required and worker ID must not be negative.");
        }
        TaskExecution execution = executions.get(taskExecutionId);
        if (execution == null || execution.getExecutionStatus() != ExecutionStatus.IN_QUEUE) {
            throw new IllegalStateException("Execution does not exist or is not IN_QUEUE: " + taskExecutionId);
        }
        execution.setWorkerId(workerId);
        execution.setExecutionStatus(ExecutionStatus.ASSIGNED);
        execution.setUpdatedAt(Instant.now());
    }

    @Override
    public synchronized boolean discardExecutionsForTask(UUID taskId) {
        Instant now = Instant.now();
        for (TaskExecution execution : executions.values()) {
            if (execution.getTaskId().equals(taskId)
                    && (execution.getExecutionStatus() == ExecutionStatus.PENDING
                            || execution.getExecutionStatus() == ExecutionStatus.IN_QUEUE)) {
                if (execution.getExecutionStatus() == ExecutionStatus.PENDING) {
                    pendingExecutions.remove(execution);
                }
                execution.setExecutionStatus(ExecutionStatus.DISCARDED);
                execution.setUpdatedAt(now);
            }
        }
        return true;
    }

    private TaskExecution snapshot(TaskExecution execution) {
        TaskExecution copy = new TaskExecution(execution.getTaskExecutionId(), execution.getTaskId(),
                execution.getTaskScheduleId(), execution.getExecutionTime(), execution.getWorkerId(),
                execution.getExecutionStatus());
        copy.setUpdatedAt(execution.getUpdatedAt());
        return copy;
    }
}
