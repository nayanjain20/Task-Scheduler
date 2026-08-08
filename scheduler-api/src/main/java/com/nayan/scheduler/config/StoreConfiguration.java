package com.nayan.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;
import com.nayan.scheduler.core.store.inmemory.TaskIMStore;
import com.nayan.scheduler.core.store.inmemory.TaskScheduleIMStore;

@Configuration
public class StoreConfiguration {
    @Bean
    public TaskStore taskStore() {
        return new TaskIMStore();
    }

    @Bean
    public TaskScheduleStore taskScheduleStore() {
        return new TaskScheduleIMStore();
    }

    @Bean
    public TaskExecutionStore taskExecutionStore() {
        return new TaskExecutionIMStore();
    }
}
