package com.nayan.scheduler.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.nayan.scheduler.core.engine.Scheduler;

class SchedulerPollingTest {
    @Test
    void pollsEverySecondAndStopsWhenInterrupted() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        List<Long> calls = new CopyOnWriteArrayList<>();
        CountDownLatch polled = new CountDownLatch(3);
        doAnswer(invocation -> {
            calls.add(System.nanoTime());
            polled.countDown();
            if (calls.size() == 3) {
                Thread.currentThread().interrupt();
            }
            return null;
        }).when(scheduler).processScheduledExecutions();
        Thread thread = new Thread(new SchedulerProcess(scheduler), "polling-interval-test");
        thread.setDaemon(true);
        thread.start();
        try {
            assertThat(polled.await(8, TimeUnit.SECONDS)).isTrue();
            thread.join(5000);
            assertThat(thread.isAlive()).isFalse();
            assertThat(SchedulerProcess.POLL_INTERVAL_MILLIS).isEqualTo(1000);
            for (int i = 1; i < calls.size(); i++) {
                assertThat(TimeUnit.NANOSECONDS.toMillis(calls.get(i) - calls.get(i - 1)))
                        .isBetween(900L, 4000L);
            }
            verify(scheduler, times(3)).processScheduledExecutions();
        } finally {
            thread.interrupt();
            thread.join(5000);
        }
    }

    @Test
    void pollFailureDoesNotStopFuturePolls() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        CountDownLatch nextPoll = new CountDownLatch(1);
        org.mockito.Mockito.doThrow(new IllegalStateException("Transient database failure")).doAnswer(invocation -> {
            nextPoll.countDown();
            Thread.currentThread().interrupt();
            return null;
        }).when(scheduler).processScheduledExecutions();
        Thread thread = new Thread(new SchedulerProcess(scheduler), "polling-retry-test");
        thread.setDaemon(true);
        thread.start();
        try {
            assertThat(nextPoll.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            thread.interrupt();
            thread.join(5000);
        }
    }
}
