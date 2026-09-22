package com.nayan.scheduler.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "task_executions", indexes = @Index(name = "idx_task_executions_due",
        columnList = "execution_status,execution_time"))
public class TaskExecutionEntity {
    @Id
    private UUID taskExecutionId;
    private UUID taskId;
    private UUID taskScheduleId;
    private Instant executionTime;
    private Integer workerId;
    @Enumerated(EnumType.STRING)
    private ExecutionStatus executionStatus;
    private Instant updatedAt;

}
