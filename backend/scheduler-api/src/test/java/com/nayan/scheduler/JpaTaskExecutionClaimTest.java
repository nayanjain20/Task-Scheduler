package com.nayan.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.persistence.repository.TaskExecutionRepository;
import com.nayan.scheduler.persistence.store.JpaTaskExecutionStore;

import jakarta.persistence.EntityManagerFactory;

@DataJpaTest(showSql = false)
@Import(JpaTaskExecutionStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaTaskExecutionClaimTest {

    @Autowired
    private JpaTaskExecutionStore store;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoSpyBean
    private TaskExecutionRepository repository;

    @AfterEach
    void cleanUp() {
        reset(repository);
        repository.deleteAll();
    }

    @Test
    void claimsAndReturnsUpdatedRowsWithOneDatabaseStatement() {
        TaskExecution execution = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        execution.setWorkerId(7);
        store.addTaskExecution(execution);

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        List<TaskExecution> claimed;
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            claimed = store.claimDueExecutions();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
        verify(repository).claimDueExecutions(any(Instant.class), eq(100));
        verify(repository, never()).findById(any(UUID.class));
        assertThat(claimed).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.getTaskExecutionId()).isEqualTo(execution.getTaskExecutionId());
            assertThat(snapshot.getTaskId()).isEqualTo(execution.getTaskId());
            assertThat(snapshot.getTaskScheduleId()).isEqualTo(execution.getTaskScheduleId());
            assertThat(snapshot.getExecutionTime()).isEqualTo(execution.getExecutionTime());
            assertThat(snapshot.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
            assertThat(snapshot.getWorkerId()).isEqualTo(-1);
            assertThat(snapshot.getUpdatedAt()).isNotNull();
        });
        var persisted = repository.findById(execution.getTaskExecutionId()).orElseThrow();
        assertThat(persisted.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
        assertThat(persisted.getWorkerId()).isEqualTo(-1);
        assertThat(claimed.getFirst().getUpdatedAt()).isEqualTo(persisted.getUpdatedAt());
    }

    @Test
    @Transactional
    void flushesPendingChangesAndDoesNotReturnStaleManagedEntities() {
        TaskExecution cancelled = store.addTaskExecution(
                new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH));
        TaskExecution ready = store.addTaskExecution(
                new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH));
        var managedCancelled = repository.findById(cancelled.getTaskExecutionId()).orElseThrow();
        var managedReady = repository.findById(ready.getTaskExecutionId()).orElseThrow();
        managedCancelled.setExecutionStatus(ExecutionStatus.DISCARDED);

        List<TaskExecution> claimed = store.claimDueExecutions();

        assertThat(claimed).singleElement().satisfies(execution -> {
            assertThat(execution.getTaskExecutionId()).isEqualTo(ready.getTaskExecutionId());
            assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
        });
        assertThat(managedReady.getExecutionStatus()).isEqualTo(ExecutionStatus.PENDING);
        assertThat(repository.findById(cancelled.getTaskExecutionId()).orElseThrow().getExecutionStatus())
                .isEqualTo(ExecutionStatus.DISCARDED);
    }

    @Test
    void concurrentPollersClaimEachExecutionExactlyOnce() throws Exception {
        List<TaskExecution> executions = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            executions.add(store.addTaskExecution(
                    new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH.plusSeconds(i))));
        }
        CountDownLatch reads = new CountDownLatch(2);
        var delegate = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            reads.countDown();
            assertThat(reads.await(10, TimeUnit.SECONDS)).isTrue();
            return delegate.answer(invocation);
        }).when(repository).claimDueExecutions(any(Instant.class), eq(100));

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(store::claimDueExecutions);
            var second = pool.submit(store::claimDueExecutions);
            List<TaskExecution> firstClaims = first.get(15, TimeUnit.SECONDS);
            List<TaskExecution> secondClaims = second.get(15, TimeUnit.SECONDS);
            assertThat(Stream.concat(firstClaims.stream(), secondClaims.stream()))
                    .extracting(TaskExecution::getTaskExecutionId)
                    .containsExactlyInAnyOrderElementsOf(
                            executions.stream().map(TaskExecution::getTaskExecutionId).toList());
            assertThat(firstClaims).extracting(TaskExecution::getExecutionTime).isSorted();
            assertThat(secondClaims).extracting(TaskExecution::getExecutionTime).isSorted();
        }
        assertThat(repository.findAll()).allSatisfy(execution -> {
            assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
            assertThat(execution.getWorkerId()).isEqualTo(-1);
        });
        assertThat(store.claimDueExecutions()).isEmpty();
    }

    @Test
    void failureAfterClaimRollsBackTheWholeBatch() {
        store.addTaskExecution(
                new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH));
        store.addTaskExecution(
                new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH.plusSeconds(1)));
        var originals = repository.findAll();
        var delegate = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            delegate.answer(invocation);
            throw new IllegalStateException("Simulated failure after batch claim");
        }).when(repository).claimDueExecutions(any(Instant.class), eq(100));

        assertThatThrownBy(store::claimDueExecutions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Simulated failure after batch claim");
        verify(repository).claimDueExecutions(any(Instant.class), eq(100));
        for (var original : originals) {
            var persisted = repository.findById(original.getTaskExecutionId()).orElseThrow();
            assertThat(persisted.getExecutionStatus()).isEqualTo(ExecutionStatus.PENDING);
            assertThat(persisted.getWorkerId()).isEqualTo(original.getWorkerId());
            assertThat(persisted.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        }
    }

    @Test
    void cancellationCannotOverwriteAConcurrentWorkerAssignment() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskExecution queued = new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH);
        queued.setExecutionStatus(ExecutionStatus.IN_QUEUE);
        store.addTaskExecution(queued);
        TaskExecution pending = store.addTaskExecution(
                new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH));
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var assignment = pool.submit(() -> {
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try {
                    store.assignTaskExecutionToWorker(queued.getTaskExecutionId(), 7);
                    return true;
                } catch (IllegalStateException rejected) {
                    return false;
                }
            });
            var cancellation = pool.submit(() -> {
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return store.discardExecutionsForTask(taskId);
            });
            start.countDown();
            boolean assigned = assignment.get(15, TimeUnit.SECONDS);
            assertThat(cancellation.get(15, TimeUnit.SECONDS)).isTrue();
            var persisted = repository.findById(queued.getTaskExecutionId()).orElseThrow();
            assertThat(persisted.getExecutionStatus())
                    .isEqualTo(assigned ? ExecutionStatus.ASSIGNED : ExecutionStatus.DISCARDED);
            if (assigned) {
                assertThat(persisted.getWorkerId()).isEqualTo(7);
            }
        }
        assertThat(repository.findById(pending.getTaskExecutionId()).orElseThrow().getExecutionStatus())
                .isEqualTo(ExecutionStatus.DISCARDED);
    }
}
