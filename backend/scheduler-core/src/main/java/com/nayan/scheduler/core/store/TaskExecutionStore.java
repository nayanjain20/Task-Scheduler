package com.nayan.scheduler.core.store;

import java.util.List;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskExecution;

public interface TaskExecutionStore {
    int CLAIM_BATCH_SIZE = 100;

    public TaskExecution addTaskExecution(TaskExecution taskExecution);

    public List<TaskExecution> getTaskExecutionsForTask(UUID taskId);

    /**
     * Atomically changes up to CLAIM_BATCH_SIZE PENDING executions scheduled
     * strictly before now to IN_QUEUE and returns only this batch's snapshots,
     * ordered by execution time then execution ID. Refreshes
     * updatedAt.
     * A concurrent poll must not claim the same execution again.
     */
    public List<TaskExecution> claimDueExecutions();

    /**
     * Returns executions of the requested status across tasks, regardless of
     * execution time.
     * The result is empty when none exist; ordering is not guaranteed.
     */
    public List<TaskExecution> getAllTaskExecutionsOfStatus(TaskExecution.ExecutionStatus status);

    public boolean updateTaskExecution(TaskExecution taskExecution);

    public boolean discardExecutionsForTask(UUID taskId);

    /**
     * Assigns an existing IN_QUEUE execution and refreshes updatedAt.
     * Rejects null execution IDs and negative worker IDs with
     * IllegalArgumentException,
     * and missing or non-IN_QUEUE executions with IllegalStateException.
     * Does not enqueue or execute the task.
     */
    public void assignTaskExecutionToWorker(UUID taskExecutionId, int workerId);

}
