package com.nayan.scheduler.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.persistence.mapper.TaskExecutionMapper;
import com.nayan.scheduler.persistence.repository.TaskExecutionRepository;

import jakarta.persistence.EntityManager;

@Repository
public class JpaTaskExecutionStore implements TaskExecutionStore {

    private final TaskExecutionRepository repository;
    private final EntityManager entityManager;

    public JpaTaskExecutionStore(TaskExecutionRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public List<TaskExecution> claimDueExecutions() {
        Instant now = Instant.now();
        // Preserve pending writes, then prevent stale managed entities masking the native UPDATE result.
        entityManager.flush();
        entityManager.clear();
        return repository.claimDueExecutions(now, CLAIM_BATCH_SIZE).stream()
                .map(TaskExecutionMapper::toModel).toList();
    }

    @Override
    public TaskExecution addTaskExecution(TaskExecution taskExecution) {
        taskExecution.setUpdatedAt(Instant.now());
        return TaskExecutionMapper.toModel(repository.save(TaskExecutionMapper.toEntity(taskExecution)));
    }

    @Override
    public List<TaskExecution> getTaskExecutionsForTask(UUID taskId) {
        return repository.findByTaskId(taskId).stream().map(TaskExecutionMapper::toModel).toList();
    }

    @Override
    public boolean updateTaskExecution(TaskExecution taskExecution) {
        if (!repository.existsById(taskExecution.getTaskExecutionId())) {
            return false;
        }
        taskExecution.setUpdatedAt(Instant.now());
        repository.save(TaskExecutionMapper.toEntity(taskExecution));
        return true;
    }

    @Override
    @Transactional
    public boolean discardExecutionsForTask(UUID taskId) {
        repository.discardUnassignedExecutions(taskId, Instant.now(),
                List.of(ExecutionStatus.PENDING, ExecutionStatus.IN_QUEUE), ExecutionStatus.DISCARDED);
        return true;
    }

    @Override
    public List<TaskExecution> getAllTaskExecutionsOfStatus(TaskExecution.ExecutionStatus status) {
        return repository.findByExecutionStatus(status).stream()
                .map(TaskExecutionMapper::toModel)
                .toList();
    }

    @Override
    @Transactional
    public void assignTaskExecutionToWorker(UUID taskExecutionId, int workerId) {
        if (taskExecutionId == null || workerId < 0) {
            throw new IllegalArgumentException("Execution ID is required and worker ID must not be negative.");
        }
        int updated = repository.assignQueuedExecution(taskExecutionId, workerId, Instant.now(),
                ExecutionStatus.IN_QUEUE, ExecutionStatus.ASSIGNED);
        if (updated != 1) {
            throw new IllegalStateException("Execution does not exist or is not IN_QUEUE: " + taskExecutionId);
        }
    }

}