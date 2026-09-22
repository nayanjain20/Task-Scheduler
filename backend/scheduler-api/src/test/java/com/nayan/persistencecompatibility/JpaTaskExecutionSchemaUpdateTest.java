package com.nayan.persistencecompatibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.Test;

import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.persistence.entity.TaskExecutionEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Keep legacy entity metadata outside the application's com.nayan.scheduler entity-scan package.
class JpaTaskExecutionSchemaUpdateTest {

    @Test
    void explicitlyMigratesLegacyFileBackedStatusEnumWhenHibernateUpdateDoesNot() throws Exception {
        String databaseName = "execution-schema-upgrade-" + UUID.randomUUID();
        String url = "jdbc:h2:file:.\\target\\" + databaseName;
        var ids = new EnumMap<LegacyStatus, UUID>(LegacyStatus.class);
        try {
            try (SessionFactory legacy = sessionFactory(url, "create", LegacyExecution.class)) {
                legacy.inTransaction(session -> {
                    for (LegacyStatus status : LegacyStatus.values()) {
                        LegacyExecution execution = new LegacyExecution();
                        execution.taskExecutionId = UUID.randomUUID();
                        execution.taskId = UUID.randomUUID();
                        execution.taskScheduleId = UUID.randomUUID();
                        execution.executionTime = Instant.EPOCH;
                        execution.workerId = -1;
                        execution.executionStatus = status;
                        execution.updatedAt = Instant.EPOCH;
                        session.persist(execution);
                        ids.put(status, execution.taskExecutionId);
                    }
                });
            }
            try (SessionFactory current = sessionFactory(url, "update", TaskExecutionEntity.class)) {
                UUID pendingId = ids.get(LegacyStatus.PENDING);
                assertThatThrownBy(() -> current.inTransaction(session -> {
                    TaskExecutionEntity execution = session.find(TaskExecutionEntity.class, pendingId);
                    execution.setExecutionStatus(ExecutionStatus.IN_QUEUE);
                })).isInstanceOf(DataException.class).hasMessageContaining("IN_QUEUE");
                current.inTransaction(session -> session.createNativeMutationQuery("""
                        ALTER TABLE task_executions ALTER COLUMN execution_status
                        ENUM ('ASSIGNED', 'COMPLETED', 'DISCARDED', 'FAILED', 'IN_QUEUE', 'PENDING', 'SKIPPED')
                        """).executeUpdate());
                current.inTransaction(session -> ids.forEach((status, id) -> {
                    TaskExecutionEntity execution = session.find(TaskExecutionEntity.class, id);
                    assertThat(execution.getExecutionStatus().name()).isEqualTo(status.name());
                    assertThat(execution.getExecutionTime()).isEqualTo(Instant.EPOCH);
                    assertThat(execution.getUpdatedAt()).isEqualTo(Instant.EPOCH);
                }));
                current.inTransaction(session -> session.find(TaskExecutionEntity.class, pendingId)
                        .setExecutionStatus(ExecutionStatus.IN_QUEUE));
                current.inTransaction(session -> assertThat(
                        session.find(TaskExecutionEntity.class, pendingId).getExecutionStatus())
                        .isEqualTo(ExecutionStatus.IN_QUEUE));
            }
        } finally {
            Files.deleteIfExists(Path.of("target", databaseName + ".mv.db"));
            Files.deleteIfExists(Path.of("target", databaseName + ".trace.db"));
        }
    }

    private SessionFactory sessionFactory(String url, String ddlMode, Class<?> entity) {
        return new Configuration()
                .addAnnotatedClass(entity)
                .setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy())
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.hbm2ddl.auto", ddlMode)
                .buildSessionFactory();
    }

    private enum LegacyStatus {
        COMPLETED, SKIPPED, PENDING, DISCARDED, FAILED, ASSIGNED
    }

    @Entity(name = "LegacyExecution")
    @Table(name = "task_executions")
    public static class LegacyExecution {
        @Id
        UUID taskExecutionId;
        UUID taskId;
        UUID taskScheduleId;
        Instant executionTime;
        Integer workerId;
        @Enumerated(EnumType.STRING)
        LegacyStatus executionStatus;
        Instant updatedAt;
    }
}
