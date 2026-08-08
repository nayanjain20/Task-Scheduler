package com.nayan.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nayan.scheduler.core.service.TaskSchedulerService;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;

@Configuration
public class EngineConfiguration {

    @Bean
    public TaskSchedulerService taskSchedulerService(TaskStore taskStore, TaskScheduleStore taskScheduleStore,
            TaskExecutionStore taskExecutionStore) {
        TaskSchedulerService service = new TaskSchedulerService(taskStore, taskScheduleStore, taskExecutionStore);
        service.startScheduler();
        return service;
    }
}
