package com.nayan.scheduler.persistence.entity;

import java.util.UUID;

import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.Task.TaskType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {
    @Id
    private UUID taskId;
    private String taskName;
    private UUID taskScheduleId;
    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;
    @Enumerated(EnumType.STRING)
    private TaskType taskType;
    private String message;
    private String filePath;
}
