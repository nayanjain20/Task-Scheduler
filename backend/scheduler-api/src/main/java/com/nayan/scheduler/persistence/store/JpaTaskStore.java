package com.nayan.scheduler.persistence.store;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.persistence.mapper.TaskMapper;
import com.nayan.scheduler.persistence.repository.TaskRepository;

@Repository
public class JpaTaskStore implements TaskStore {

    private final TaskRepository repository;

    public JpaTaskStore(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public Task getTask(UUID taskId) {
        return repository.findById(taskId).map(TaskMapper::toModel).orElse(null);
    }

    @Override
    public List<Task> getAllTasks() {
        return repository.findAll().stream().map(TaskMapper::toModel).toList();
    }

    @Override
    public List<Task> getAllActiveTasks() {
        return repository.findByTaskStatus(Task.TaskStatus.ACTIVE).stream().map(TaskMapper::toModel).toList();
    }

    @Override
    public void addTask(Task task) {
        repository.save(TaskMapper.toEntity(task));
    }

    @Override
    public boolean updateTask(Task task) {
        if (!repository.existsById(task.getTaskId())) {
            return false;
        }

        repository.save(TaskMapper.toEntity(task));
        return true;
    }
}