package com.nayan.scheduler.persistence.store;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.persistence.entity.TaskExecutionEntity;
import com.nayan.scheduler.persistence.mapper.TaskExecutionMapper;
import com.nayan.scheduler.persistence.repository.TaskExecutionRepository;

@Repository
public class JpaTaskExecutionStore implements TaskExecutionStore {

    private final TaskExecutionRepository repository;

    public JpaTaskExecutionStore(TaskExecutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskExecution addTaskExecution(TaskExecution taskExecution) {
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
        repository.save(TaskExecutionMapper.toEntity(taskExecution));
        return true;
    }

    @Override
    @Transactional
    public boolean discardExecutionsForTask(UUID taskId) {
        List<TaskExecutionEntity> executions = repository.findByTaskId(taskId);
        executions.stream()
                .filter(execution -> execution.getExecutionStatus() == ExecutionStatus.PENDING)
                .forEach(execution -> execution.setExecutionStatus(ExecutionStatus.DISCARDED));
        repository.saveAll(executions);
        return true;
    }
}