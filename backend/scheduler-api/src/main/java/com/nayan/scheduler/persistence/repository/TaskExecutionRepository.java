package com.nayan.scheduler.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nayan.scheduler.persistence.entity.TaskExecutionEntity;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, UUID> {
    List<TaskExecutionEntity> findByTaskId(UUID taskId);
}
