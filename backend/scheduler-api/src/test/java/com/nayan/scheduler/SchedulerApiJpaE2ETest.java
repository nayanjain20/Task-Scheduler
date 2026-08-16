package com.nayan.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.TaskExecution.ExecutionStatus;
import com.nayan.scheduler.persistence.repository.TaskExecutionRepository;
import com.nayan.scheduler.persistence.repository.TaskRepository;
import com.nayan.scheduler.persistence.repository.TaskScheduleRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SchedulerApiJpaE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskScheduleRepository taskScheduleRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @BeforeEach
    void clearDatabase() {
        taskExecutionRepository.deleteAll();
        taskScheduleRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    void persistsTaskAndStateTransitionsThroughHttpApi() throws Exception {
        String response = mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "type": "PRINT",
                          "taskName": "Persisted task",
                          "schedule": {
                            "startTime": "2099-01-01T00:00:00Z",
                            "interval": 3600,
                            "recurring": false
                          }
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseBody = objectMapper.readTree(response);
        UUID taskId = UUID.fromString(responseBody.get("taskId").asText());

        assertThat(taskRepository.findById(taskId)).isPresent();
        assertThat(taskScheduleRepository.count()).isEqualTo(1);
        assertThat(taskExecutionRepository.findByTaskId(taskId))
                .singleElement()
                .extracting(execution -> execution.getExecutionStatus())
                .isEqualTo(ExecutionStatus.PENDING);

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.tasks[0].taskId").value(taskId.toString()));

        mockMvc.perform(post("/tasks/{taskId}/pause", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertThat(taskRepository.findById(taskId).orElseThrow().getTaskStatus()).isEqualTo(TaskStatus.PAUSE);
        assertThat(taskExecutionRepository.findByTaskId(taskId))
                .allMatch(execution -> execution.getExecutionStatus() == ExecutionStatus.DISCARDED);

        mockMvc.perform(post("/tasks/{taskId}/resume", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertThat(taskRepository.findById(taskId).orElseThrow().getTaskStatus()).isEqualTo(TaskStatus.ACTIVE);
        assertThat(taskExecutionRepository.findByTaskId(taskId)).hasSize(2);

        mockMvc.perform(post("/tasks/{taskId}/cancel", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertThat(taskRepository.findById(taskId).orElseThrow().getTaskStatus()).isEqualTo(TaskStatus.CANCEL);
        assertThat(taskExecutionRepository.findByTaskId(taskId))
                .allMatch(execution -> execution.getExecutionStatus() == ExecutionStatus.DISCARDED);
    }

    @Test
    void persistsTaskSubtypePayloads() throws Exception {
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                                "type": "WRITE",
                                "taskName": "Write persisted file",
                                "schedule": { "interval": 3600, "recurring": false },
                                "payload": { "filePath": "temp/jpa.txt", "message": "stored message" }
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").isNotEmpty());

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                                "type": "DELETE",
                                "taskName": "Delete persisted file",
                                "schedule": { "interval": 3600, "recurring": false },
                                "payload": { "filePath": "temp/jpa.txt" }
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").isNotEmpty());

        assertThat(taskRepository.findAll())
                .anySatisfy(task -> {
                    assertThat(task.getFilePath()).isEqualTo("temp/jpa.txt");
                    assertThat(task.getMessage()).isEqualTo("stored message");
                })
                .anySatisfy(task -> {
                    assertThat(task.getFilePath()).isEqualTo("temp/jpa.txt");
                    assertThat(task.getMessage()).isNull();
                });

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }
}