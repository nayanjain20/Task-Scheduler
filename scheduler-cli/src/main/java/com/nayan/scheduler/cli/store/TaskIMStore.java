package com.nayan.scheduler.cli.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.store.TaskStore;

public class TaskIMStore implements TaskStore {

    Map<UUID, Task> taskMap;
    List<Task> taskList;

    public TaskIMStore() {
        this.taskMap = new HashMap<>();
        this.taskList = new ArrayList<>();
    }

    @Override
    public Task getTask(UUID taskId) {
        return taskMap.get(taskId);
    }

    @Override
    public List<Task> getAllTasks() {
        return taskList;
    }

    @Override
    public List<Task> getAllActiveTasks() {
        return taskList.stream().filter(task -> task.getTaskStatus().equals(TaskStatus.ACTIVE)).toList();
    }

    @Override
    public void addTask(Task task) {
        taskMap.put(task.getTaskId(), task);
        taskList.add(task);
    }

    @Override
    public boolean updateTask(Task task) {
        taskMap.put(task.getTaskId(), task);
        taskList = new ArrayList(taskList.stream().filter(t -> (!t.getTaskId().equals(task.getTaskId()))).toList());
        taskList.add(task);
        return true;
    }

}
