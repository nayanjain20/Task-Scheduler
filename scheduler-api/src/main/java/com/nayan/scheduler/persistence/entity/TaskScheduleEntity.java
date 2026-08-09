package com.nayan.scheduler.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskScheduleEntity {

    @Id
    private UUID taskScheduleId;

    private UUID taskId;

    private Instant startTime;

    private boolean recurring;

    private int intervalSeconds;
}