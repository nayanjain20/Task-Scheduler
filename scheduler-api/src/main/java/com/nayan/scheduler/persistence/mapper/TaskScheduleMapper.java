package com.nayan.scheduler.persistence.mapper;

import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.persistence.entity.TaskScheduleEntity;

public final class TaskScheduleMapper {

    private TaskScheduleMapper() {
    }

    public static TaskScheduleEntity toEntity(TaskSchedule schedule) {
        return new TaskScheduleEntity(
                schedule.getTaskScheduleId(),
                schedule.getTaskID(),
                schedule.getStartTime(),
                schedule.isRecurring(),
                schedule.getIntervalSeconds());
    }

    public static TaskSchedule toModel(TaskScheduleEntity entity) {
        return new TaskSchedule(
                entity.getTaskScheduleId(),
                entity.getTaskId(),
                entity.getStartTime(),
                entity.isRecurring(),
                entity.getIntervalSeconds());
    }
}