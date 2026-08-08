package com.nayan.scheduler.core.store;

import java.util.UUID;

import com.nayan.scheduler.core.model.TaskSchedule;

public interface TaskScheduleStore {
    public TaskSchedule addTaskSchedule(TaskSchedule taskSchedule);

    public TaskSchedule getTaskSchedule(UUID taskScheId);

}