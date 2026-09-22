package com.nayan.scheduler.core.engine;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.util.Logger;

public class Scheduler {

    private final Executor executor;
    private final TaskExecutionStore taskExecutionStore;

    public Scheduler(Executor executor, TaskExecutionStore taskExecutionStore) {
        this.executor = executor;
        this.taskExecutionStore = taskExecutionStore;
    }

    public void processScheduledExecutions() {
        for (TaskExecution execution : taskExecutionStore.claimDueExecutions()) {
            try {
                executor.addScheduledExecution(execution);
            } catch (RuntimeException e) {
                Logger.log("[SCHEDULER] Could not dispatch execution " + execution.getTaskExecutionId() + ": " + e);
            }
        }
    }
}
