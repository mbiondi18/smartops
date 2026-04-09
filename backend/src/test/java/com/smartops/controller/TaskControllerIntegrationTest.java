package com.smartops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.dto.AuthDTO;
import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.repository.TaskRepository;
import com.smartops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Autowired
    private UserRepository userRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // register a test user and get the JWT token
        AuthDTO.RegisterRequest registerRequest = new AuthDTO.RegisterRequest(
                "Test User", "test@example.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthDTO.AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthDTO.AuthResponse.class);
        authToken = "Bearer " + authResponse.getToken();
    }

    @Test
    @DisplayName("POST /api/tasks - should create task and return 201")
    void createTask_Returns201() throws Exception {
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("My Task", "Some description", Task.Priority.HIGH);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("My Task")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/tasks - should return 403 without token")
    void createTask_NoToken_Returns403() throws Exception {
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("My Task", "desc", Task.Priority.HIGH);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/tasks - should return 400 when title is missing")
    void createTask_MissingTitle_Returns400() throws Exception {
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("", null, null);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/tasks - should return list of tasks for the logged-in user")
    void getAllTasks_ReturnsList() throws Exception {
        // create a task first
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("Task 1", "desc", Task.Priority.LOW);
        mockMvc.perform(post("/api/tasks")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task 1")));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - should return 404 when not found")
    void getTaskById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/9999")
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - should update task successfully")
    void updateTask_Success() throws Exception {
        // create task first
        TaskDTO.CreateRequest createReq = new TaskDTO.CreateRequest("Original", "desc", Task.Priority.LOW);
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        TaskDTO.Response created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), TaskDTO.Response.class);

        TaskDTO.UpdateRequest update = new TaskDTO.UpdateRequest(
                "Updated", null, Task.Priority.HIGH, Task.Status.IN_PROGRESS, null);

        mockMvc.perform(put("/api/tasks/" + created.getId())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - should delete task and return 204")
    void deleteTask_Success() throws Exception {
        // create task first
        TaskDTO.CreateRequest createReq = new TaskDTO.CreateRequest("To Delete", "desc", Task.Priority.LOW);
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        TaskDTO.Response created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), TaskDTO.Response.class);

        mockMvc.perform(delete("/api/tasks/" + created.getId())
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + created.getId())
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }
}
