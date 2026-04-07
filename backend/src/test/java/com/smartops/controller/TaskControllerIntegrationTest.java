package com.smartops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/tasks - should create task and return 201")
    void createTask_Returns201() throws Exception {
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("My Task", "Some description", Task.Priority.HIGH);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("My Task")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when title is missing")
    void createTask_MissingTitle_Returns400() throws Exception {
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("", null, null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/tasks - should return list of tasks")
    void getAllTasks_ReturnsList() throws Exception {
        Task task = Task.builder()
                .title("Task 1")
                .priority(Task.Priority.LOW)
                .status(Task.Status.PENDING)
                .aiAnalysed(false)
                .build();
        taskRepository.save(task);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task 1")));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - should return 404 when not found")
    void getTaskById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update task successfully")
    void updateTask_Success() throws Exception {
        Task task = taskRepository.save(Task.builder()
                .title("Original")
                .priority(Task.Priority.LOW)
                .status(Task.Status.PENDING)
                .aiAnalysed(false)
                .build());

        TaskDTO.UpdateRequest update = new TaskDTO.UpdateRequest(
                "Updated", null, Task.Priority.HIGH, Task.Status.IN_PROGRESS, null);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - should delete task and return 204")
    void deleteTask_Success() throws Exception {
        Task task = taskRepository.save(Task.builder()
                .title("To Delete")
                .priority(Task.Priority.LOW)
                .status(Task.Status.PENDING)
                .aiAnalysed(false)
                .build());

        mockMvc.perform(delete("/api/tasks/" + task.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + task.getId()))
                .andExpect(status().isNotFound());
    }
}
