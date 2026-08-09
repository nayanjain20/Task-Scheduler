package com.nayan.scheduler.persistence.store;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.persistence.mapper.TaskScheduleMapper;
import com.nayan.scheduler.persistence.repository.TaskScheduleRepository;

@Repository
public class JpaTaskScheduleStore implements TaskScheduleStore {

    private final TaskScheduleRepository repository;

    public JpaTaskScheduleStore(TaskScheduleRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskSchedule addTaskSchedule(TaskSchedule taskSchedule) {
        return TaskScheduleMapper.toModel(repository.save(TaskScheduleMapper.toEntity(taskSchedule)));
    }

    @Override
    public TaskSchedule getTaskSchedule(UUID taskScheduleId) {
        return repository.findById(taskScheduleId).map(TaskScheduleMapper::toModel).orElse(null);
    }
}