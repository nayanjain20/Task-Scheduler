package com.nayan.scheduler.dto;

import java.time.Instant;

import lombok.Getter;

@Getter
public class ScheduleRequest {
    private Instant startTime;
    private int interval;
    private boolean recurring;

}
