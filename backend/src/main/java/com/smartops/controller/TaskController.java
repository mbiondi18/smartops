package com.smartops.controller;

import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.service.AiAnalysisService;
import com.smartops.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;
    private final AiAnalysisService aiAnalysisService;

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskDTO.Response> createTask(@Valid @RequestBody TaskDTO.CreateRequest request) {
        TaskDTO.Response response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all tasks")
    public ResponseEntity<List<TaskDTO.Response>> getAllTasks(
            @RequestParam(required = false) Task.Status status) {
        List<TaskDTO.Response> tasks = status != null
                ? taskService.getTasksByStatus(status)
                : taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskDTO.Response> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskDTO.Response> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO.UpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/analyse")
    @Operation(summary = "Trigger AI analysis on a task")
    public ResponseEntity<TaskDTO.AnalysisResponse> analyseTask(@PathVariable Long id) {
        TaskDTO.AnalysisResponse analysis = aiAnalysisService.analyseTask(id);
        taskService.applyAiAnalysis(id, analysis);
        return ResponseEntity.ok(analysis);
    }
}
