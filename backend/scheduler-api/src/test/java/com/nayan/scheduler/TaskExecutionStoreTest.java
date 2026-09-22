package com.nayan.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.inmemory.TaskExecutionIMStore;
import com.nayan.scheduler.persistence.store.JpaTaskExecutionStore;

@DataJpaTest
@Import(JpaTaskExecutionStore.class)
class TaskExecutionStoreTest {

    @Autowired
    private JpaTaskExecutionStore jpaStore;

    @Test
    void assignsQueuedExecutionInMemory() {
        verifyAssignment(new TaskExecutionIMStore());
    }

    @Test
    void assignsQueuedExecutionInJpa() {
        verifyAssignment(jpaStore);
    }

    private void verifyAssignment(TaskExecutionStore store) {
        Instant before = Instant.parse("2026-09-20T08:00:00Z");
        Instant assignedAt = before.plusSeconds(10);
        Instant later = assignedAt.plusSeconds(10);
        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            TaskExecution execution = saveAt(store, time, before, ExecutionStatus.IN_QUEUE);
            TaskExecution unrelated = saveAt(store, time, before, ExecutionStatus.PENDING);

            time.when(Instant::now).thenReturn(assignedAt);
            store.assignTaskExecutionToWorker(execution.getTaskExecutionId(), 0);

            TaskExecution assigned = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
            assertThat(assigned.getTaskExecutionId()).isEqualTo(execution.getTaskExecutionId());
            assertThat(assigned.getTaskScheduleId()).isEqualTo(execution.getTaskScheduleId());
            assertThat(assigned.getExecutionTime()).isEqualTo(execution.getExecutionTime());
            assertThat(assigned.getExecutionStatus()).isEqualTo(ExecutionStatus.ASSIGNED);
            assertThat(assigned.getWorkerId()).isZero();
            assertThat(assigned.getUpdatedAt()).isEqualTo(assignedAt);
            time.when(Instant::now).thenReturn(later);
            assertThatThrownBy(() -> store.assignTaskExecutionToWorker(execution.getTaskExecutionId(), 1))
                    .isInstanceOf(IllegalStateException.class);
            TaskExecution stillAssigned = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
            assertThat(stillAssigned.getWorkerId()).isZero();
            assertThat(stillAssigned.getUpdatedAt()).isEqualTo(assignedAt);
            TaskExecution untouched = store.getTaskExecutionsForTask(unrelated.getTaskId()).getFirst();
            assertThat(untouched.getExecutionStatus()).isEqualTo(ExecutionStatus.PENDING);
            assertThat(untouched.getWorkerId()).isEqualTo(3);
            assertThat(untouched.getUpdatedAt()).isEqualTo(before);
        }
    }

    @Test
    void rejectsInvalidAssignmentsInMemory() {
        verifyInvalidAssignments(new TaskExecutionIMStore());
    }

    @Test
    void rejectsInvalidAssignmentsInJpa() {
        verifyInvalidAssignments(jpaStore);
    }

    private void verifyInvalidAssignments(TaskExecutionStore store) {
        assertThatThrownBy(() -> store.assignTaskExecutionToWorker(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.assignTaskExecutionToWorker(UUID.randomUUID(), 0))
                .isInstanceOf(IllegalStateException.class);

        Instant now = Instant.parse("2026-09-20T08:00:00Z");
        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            TaskExecution pending = saveAt(store, time, now, ExecutionStatus.PENDING);
            assertThatThrownBy(() -> store.assignTaskExecutionToWorker(pending.getTaskExecutionId(), -1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(store.getTaskExecutionsForTask(pending.getTaskId()).getFirst().getExecutionStatus())
                    .isEqualTo(ExecutionStatus.PENDING);
            for (ExecutionStatus status : ExecutionStatus.values()) {
                if (status != ExecutionStatus.IN_QUEUE) {
                    TaskExecution execution = saveAt(store, time, now, status);
                    assertThatThrownBy(() -> store.assignTaskExecutionToWorker(execution.getTaskExecutionId(), 0))
                            .isInstanceOf(IllegalStateException.class);
                    TaskExecution unchanged = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
                    assertThat(unchanged.getExecutionStatus()).isEqualTo(status);
                    assertThat(unchanged.getWorkerId()).isEqualTo(3);
                    assertThat(unchanged.getUpdatedAt()).isEqualTo(now);
                }
            }
        }
    }

    @Test
    void claimsAllDueExecutionsInOrderInMemory() {
        verifyDueClaims(new TaskExecutionIMStore());
    }

    @Test
    void claimsAllDueExecutionsInOrderInJpa() {
        verifyDueClaims(jpaStore);
    }

    private void verifyDueClaims(TaskExecutionStore store) {
        Instant now = Instant.parse("2026-09-20T08:00:00Z");
        Instant before = now.minusSeconds(20);
        Instant next = now.plusMillis(1);
        Instant afterNext = now.plusMillis(2);
        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            assertThat(store.claimDueExecutions()).isEmpty();
            time.when(Instant::now).thenReturn(before);
            TaskExecution latest = store.addTaskExecution(
                    new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), now.minusMillis(1)));
            TaskExecution earliest = store.addTaskExecution(
                    new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), now.minusSeconds(10)));
            TaskExecution middle = store.addTaskExecution(
                    new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), now.minusSeconds(5)));
            TaskExecution boundary = store.addTaskExecution(
                    new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), now));
            TaskExecution future = store.addTaskExecution(
                    new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), now.plusMillis(1)));
            for (ExecutionStatus status : ExecutionStatus.values()) {
                if (status != ExecutionStatus.PENDING) {
                    saveAt(store, time, before, status);
                }
            }
            time.when(Instant::now).thenReturn(now);
            List<TaskExecution> claimed = store.claimDueExecutions();
            assertThat(claimed).extracting(TaskExecution::getTaskExecutionId)
                    .containsExactly(earliest.getTaskExecutionId(), middle.getTaskExecutionId(),
                            latest.getTaskExecutionId());
            assertThat(claimed).allSatisfy(execution -> {
                assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
                assertThat(execution.getWorkerId()).isEqualTo(-1);
                assertThat(execution.getUpdatedAt()).isEqualTo(now);
                TaskExecution persisted = store.getTaskExecutionsForTask(execution.getTaskId()).getFirst();
                assertThat(persisted.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
                assertThat(persisted.getUpdatedAt()).isEqualTo(now);
                assertThat(persisted.getWorkerId()).isEqualTo(-1);
            });
            assertThat(store.claimDueExecutions()).isEmpty();
            assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING))
                    .extracting(TaskExecution::getTaskExecutionId)
                    .containsExactlyInAnyOrder(boundary.getTaskExecutionId(), future.getTaskExecutionId());
            time.when(Instant::now).thenReturn(next);
            assertThat(store.claimDueExecutions()).extracting(TaskExecution::getTaskExecutionId)
                    .containsExactly(boundary.getTaskExecutionId());
            time.when(Instant::now).thenReturn(afterNext);
            assertThat(store.claimDueExecutions()).extracting(TaskExecution::getTaskExecutionId)
                    .containsExactly(future.getTaskExecutionId());
        }
    }

    @Test
    void claimsAtMostOneHundredExecutionsPerPollInMemory() {
        verifyBoundedClaims(new TaskExecutionIMStore());
    }

    @Test
    void claimsAtMostOneHundredExecutionsPerPollInJpa() {
        verifyBoundedClaims(jpaStore);
    }

    private void verifyBoundedClaims(TaskExecutionStore store) {
        assertThat(TaskExecutionStore.CLAIM_BATCH_SIZE).isEqualTo(100);
        List<UUID> expectedIds = new ArrayList<>();
        for (int i = 204; i >= 0; i--) {
            UUID id = new UUID(0, i + 1);
            expectedIds.addFirst(id);
            store.addTaskExecution(new TaskExecution(id, UUID.randomUUID(), UUID.randomUUID(),
                    Instant.EPOCH.plusSeconds(i / 3), 7, ExecutionStatus.PENDING));
        }
        TaskExecution alreadyQueued = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        alreadyQueued.setExecutionStatus(ExecutionStatus.IN_QUEUE);
        store.addTaskExecution(alreadyQueued);
        TaskExecution future = store.addTaskExecution(new TaskExecution(UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2099-01-01T00:00:00Z")));

        int offset = 0;
        for (int size : List.of(100, 100, 5)) {
            List<TaskExecution> batch = store.claimDueExecutions();
            assertThat(batch).extracting(TaskExecution::getTaskExecutionId)
                    .containsExactlyElementsOf(expectedIds.subList(offset, offset + size));
            assertThat(batch).allSatisfy(execution -> {
                assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.IN_QUEUE);
                assertThat(execution.getWorkerId()).isEqualTo(-1);
                assertThat(execution.getUpdatedAt()).isNotNull();
            });
            offset += size;
            assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING)).hasSize(206 - offset);
        }
        assertThat(store.claimDueExecutions()).isEmpty();
        assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING))
                .extracting(TaskExecution::getTaskExecutionId).containsExactly(future.getTaskExecutionId());
        assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.IN_QUEUE)).hasSize(206);
    }

    @Test
    void findsPendingExecutionsInMemory() {
        verifyPendingExecutions(new TaskExecutionIMStore());
    }

    @Test
    void findsPendingExecutionsInJpa() {
        verifyPendingExecutions(jpaStore);
    }

    private void verifyPendingExecutions(TaskExecutionStore store) {
        assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING)).isEmpty();

        UUID taskId = UUID.randomUUID();
        TaskExecution overdue = new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH);
        TaskExecution future = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2099-01-01T00:00:00Z"));
        store.addTaskExecution(overdue);
        store.addTaskExecution(future);

        for (ExecutionStatus status : ExecutionStatus.values()) {
            if (status != ExecutionStatus.PENDING) {
                TaskExecution execution = new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH);
                execution.setExecutionStatus(status);
                store.addTaskExecution(execution);
            }
        }

        assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING))
                .allMatch(execution -> execution.getExecutionStatus() == ExecutionStatus.PENDING)
                .extracting(TaskExecution::getTaskExecutionId)
                .containsExactlyInAnyOrder(overdue.getTaskExecutionId(), future.getTaskExecutionId());

        overdue.setExecutionStatus(ExecutionStatus.COMPLETED);
        store.updateTaskExecution(overdue);
        assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING))
                .extracting(TaskExecution::getTaskExecutionId)
                .containsExactly(future.getTaskExecutionId());

        future.setExecutionStatus(ExecutionStatus.DISCARDED);
        store.updateTaskExecution(future);
        assertThat(store.getAllTaskExecutionsOfStatus(ExecutionStatus.PENDING)).isEmpty();
    }

    private TaskExecution saveAt(TaskExecutionStore store, MockedStatic<Instant> currentTime, Instant time,
            ExecutionStatus status) {
        currentTime.when(Instant::now).thenReturn(time);
        TaskExecution execution = new TaskExecution(UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
        execution.setExecutionStatus(status);
        execution.setWorkerId(3);
        TaskExecution saved = store.addTaskExecution(execution);
        assertThat(saved.getUpdatedAt()).isEqualTo(time);
        return saved;
    }

    @Test
    void refreshesOnlyDiscardedExecutionTimestampsInMemory() {
        verifyDiscardTimestamps(new TaskExecutionIMStore());
    }

    @Test
    void refreshesOnlyDiscardedExecutionTimestampsInJpa() {
        verifyDiscardTimestamps(jpaStore);
    }

    private void verifyDiscardTimestamps(TaskExecutionStore store) {
        Instant before = Instant.parse("2026-09-19T12:00:00Z");
        Instant after = before.plusSeconds(10);
        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            verifyDiscardTimestamps(store, time, before, after);
        }
    }

    private void verifyDiscardTimestamps(TaskExecutionStore store, MockedStatic<Instant> time,
            Instant before, Instant after) {
        time.when(Instant::now).thenReturn(before);
        UUID taskId = UUID.randomUUID();
        TaskExecution pending = new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH);
        TaskExecution queued = new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH);
        queued.setExecutionStatus(ExecutionStatus.IN_QUEUE);
        TaskExecution assigned = new TaskExecution(taskId, UUID.randomUUID(), Instant.EPOCH);
        assigned.setExecutionStatus(ExecutionStatus.ASSIGNED);
        store.addTaskExecution(pending);
        store.addTaskExecution(queued);
        store.addTaskExecution(assigned);
        time.when(Instant::now).thenReturn(after);
        assertThat(store.discardExecutionsForTask(taskId)).isTrue();
        assertThat(store.getTaskExecutionsForTask(taskId))
                .anySatisfy(execution -> {
                    assertThat(execution.getTaskExecutionId()).isEqualTo(queued.getTaskExecutionId());
                    assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.DISCARDED);
                    assertThat(execution.getUpdatedAt()).isEqualTo(after);
                })
                .anySatisfy(execution -> {
                    assertThat(execution.getTaskExecutionId()).isEqualTo(pending.getTaskExecutionId());
                    assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.DISCARDED);
                    assertThat(execution.getUpdatedAt()).isEqualTo(after);
                })
                .anySatisfy(execution -> {
                    assertThat(execution.getTaskExecutionId()).isEqualTo(assigned.getTaskExecutionId());
                    assertThat(execution.getExecutionStatus()).isEqualTo(ExecutionStatus.ASSIGNED);
                    assertThat(execution.getUpdatedAt()).isEqualTo(before);
                });
        assertThat(store.claimDueExecutions()).isEmpty();
        assertThatThrownBy(() -> store.assignTaskExecutionToWorker(queued.getTaskExecutionId(), 0))
                .isInstanceOf(IllegalStateException.class);
    }
}
