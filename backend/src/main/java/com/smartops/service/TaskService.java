package com.smartops.service;

import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.model.User;
import com.smartops.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskDTO.Response createTask(TaskDTO.CreateRequest request, User owner) {
        log.info("Creating task for user {}: {}", owner.getEmail(), request.getTitle());

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
                .status(Task.Status.PENDING)
                .owner(owner)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Task created with id: {}", saved.getId());
        return TaskDTO.Response.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO.Response> getAllTasks(User owner) {
        return taskRepository.findByOwner(owner)
                .stream()
                .map(TaskDTO.Response::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO.Response getTaskById(Long id, User owner) {
        Task task = findTaskOrThrow(id, owner);
        return TaskDTO.Response.fromEntity(task);
    }

    @Transactional
    public TaskDTO.Response updateTask(Long id, TaskDTO.UpdateRequest request, User owner) {
        log.info("Updating task {} for user {}", id, owner.getEmail());
        Task task = findTaskOrThrow(id, owner);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getCategory() != null) task.setCategory(request.getCategory());

        Task updated = taskRepository.save(task);
        return TaskDTO.Response.fromEntity(updated);
    }

    @Transactional
    public void deleteTask(Long id, User owner) {
        log.info("Deleting task {} for user {}", id, owner.getEmail());
        Task task = findTaskOrThrow(id, owner);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO.Response> getTasksByStatus(Task.Status status, User owner) {
        return taskRepository.findByOwnerAndStatus(owner, status)
                .stream()
                .map(TaskDTO.Response::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void applyAiAnalysis(Long taskId, TaskDTO.AnalysisResponse analysis) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));
        task.setAiSummary(analysis.getSummary());
        task.setPriority(analysis.getSuggestedPriority());
        task.setCategory(analysis.getSuggestedCategory());
        task.setAiAnalysed(true);
        taskRepository.save(task);
        log.info("AI analysis applied to task {}", taskId);
    }

    public Task findTaskOrThrow(Long id, User owner) {
        return taskRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
    }

    // used by AiAnalysisService scheduled job (no owner context)
    public Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
    }
}
