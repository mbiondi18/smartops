package com.smartops.controller;

import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.model.User;
import com.smartops.service.AiAnalysisService;
import com.smartops.service.TaskService;
import com.smartops.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private final AiAnalysisService aiAnalysisService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskDTO.Response> createTask(
            @Valid @RequestBody TaskDTO.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, user));
    }

    @GetMapping
    @Operation(summary = "Get all your tasks")
    public ResponseEntity<List<TaskDTO.Response>> getAllTasks(
            @RequestParam(required = false) Task.Status status,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<TaskDTO.Response> tasks = status != null
                ? taskService.getTasksByStatus(status, user)
                : taskService.getAllTasks(user);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public ResponseEntity<TaskDTO.Response> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(taskService.getTaskById(id, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskDTO.Response> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(taskService.updateTask(id, request, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/analyse")
    @Operation(summary = "Trigger AI analysis on a task")
    public ResponseEntity<TaskDTO.AnalysisResponse> analyseTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        // verify ownership before analysing
        taskService.getTaskById(id, user);
        TaskDTO.AnalysisResponse analysis = aiAnalysisService.analyseTask(id);
        taskService.applyAiAnalysis(id, analysis);
        return ResponseEntity.ok(analysis);
    }
}
