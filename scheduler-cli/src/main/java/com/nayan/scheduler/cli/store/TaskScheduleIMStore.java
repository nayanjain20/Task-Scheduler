package com.nayan.scheduler.cli.store;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.store.TaskScheduleStore;

public class TaskScheduleIMStore implements TaskScheduleStore {

    Map<UUID, TaskSchedule> taskScheduleMap;

    public TaskScheduleIMStore() {
        taskScheduleMap = new HashMap<>();
    }

    @Override
    public TaskSchedule addTaskSchedule(TaskSchedule taskSchedule) {
        taskScheduleMap.put(taskSchedule.getTaskScheduleId(), taskSchedule);
        return taskSchedule;
    }

    @Override
    public TaskSchedule getTaskSchedule(UUID taskScheduleId) {
        return taskScheduleMap.get(taskScheduleId);
    }

}
