package com.nayan.scheduler.core.store;

import java.util.List;
import java.util.UUID;

import com.nayan.scheduler.core.model.Task;

public interface TaskStore {
    public Task getTask(UUID taskId);

    public List<Task> getAllTasks();

    public List<Task> getAllActiveTasks();

    public void addTask(Task task);

    public boolean updateTask(Task task);

}
