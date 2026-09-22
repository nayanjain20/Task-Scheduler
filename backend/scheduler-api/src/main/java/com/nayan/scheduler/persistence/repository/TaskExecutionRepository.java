package com.nayan.scheduler.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nayan.scheduler.persistence.entity.TaskExecutionEntity;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, UUID> {
    List<TaskExecutionEntity> findByTaskId(UUID taskId);

    List<TaskExecutionEntity> findByExecutionStatus(ExecutionStatus executionStatus);

    // H2's FINAL TABLE returns exactly the rows changed by the enclosed UPDATE.
    @Query(value = """
            select claimed.* from final table (
                update task_executions
                set worker_id = -1,
                    execution_status = 'IN_QUEUE',
                    updated_at = :claimedAt
                where execution_status = 'PENDING'
                    and execution_time < :claimedAt
                    and task_execution_id in (
                        select task_execution_id from task_executions
                        where execution_status = 'PENDING' and execution_time < :claimedAt
                        order by execution_time, task_execution_id
                        fetch first :batchSize rows only
                    )
            ) claimed
            order by claimed.execution_time, claimed.task_execution_id
            """, nativeQuery = true)
    List<TaskExecutionEntity> claimDueExecutions(@Param("claimedAt") Instant claimedAt,
            @Param("batchSize") int batchSize);

        //     If taks is cancelled then simply DISCARD the executions
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TaskExecutionEntity execution
            set execution.executionStatus = :discardedStatus,
                execution.updatedAt = :updatedAt
            where execution.taskId = :taskId
                and execution.executionStatus in :cancellableStatuses
            """)
    int discardUnassignedExecutions(@Param("taskId") UUID taskId,
            @Param("updatedAt") Instant updatedAt,
            @Param("cancellableStatuses") List<ExecutionStatus> cancellableStatuses,
            @Param("discardedStatus") ExecutionStatus discardedStatus);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TaskExecutionEntity execution
            set execution.workerId = :workerId,
                execution.executionStatus = :assignedStatus,
                execution.updatedAt = :updatedAt
            where execution.taskExecutionId = :taskExecutionId
                and execution.executionStatus = :queuedStatus
            """)
    int assignQueuedExecution(@Param("taskExecutionId") UUID taskExecutionId,
            @Param("workerId") int workerId,
            @Param("updatedAt") Instant updatedAt,
            @Param("queuedStatus") ExecutionStatus queuedStatus,
            @Param("assignedStatus") ExecutionStatus assignedStatus);

}
