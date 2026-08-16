package com.nayan.scheduler.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nayan.scheduler.persistence.entity.TaskEntity;
import com.nayan.scheduler.core.model.Task.TaskStatus;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByTaskStatus(TaskStatus status);
}