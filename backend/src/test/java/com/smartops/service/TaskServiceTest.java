package com.smartops.service;

import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.model.User;
import com.smartops.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("hashedpassword")
                .build();

        sampleTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test description")
                .priority(Task.Priority.MEDIUM)
                .status(Task.Status.PENDING)
                .aiAnalysed(false)
                .owner(sampleUser)
                .build();
    }

    @Test
    @DisplayName("Should create a task successfully")
    void createTask_Success() {
        TaskDTO.CreateRequest request = new TaskDTO.CreateRequest("Test Task", "Test description", Task.Priority.HIGH);
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskDTO.Response response = taskService.createTask(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Task");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Should return all tasks for the user")
    void getAllTasks_ReturnsList() {
        when(taskRepository.findByOwner(sampleUser)).thenReturn(List.of(sampleTask));

        List<TaskDTO.Response> tasks = taskService.getAllTasks(sampleUser);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("Should return task by ID for the owner")
    void getTaskById_Found() {
        when(taskRepository.findByIdAndOwner(1L, sampleUser)).thenReturn(Optional.of(sampleTask));

        TaskDTO.Response response = taskService.getTaskById(1L, sampleUser);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when task not found")
    void getTaskById_NotFound() {
        when(taskRepository.findByIdAndOwner(99L, sampleUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L, sampleUser))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should update task fields correctly")
    void updateTask_Success() {
        TaskDTO.UpdateRequest request = new TaskDTO.UpdateRequest(
                "Updated Title", null, Task.Priority.HIGH, Task.Status.IN_PROGRESS, null);

        when(taskRepository.findByIdAndOwner(1L, sampleUser)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        taskService.updateTask(1L, request, sampleUser);

        verify(taskRepository).save(argThat(task ->
                task.getTitle().equals("Updated Title") &&
                task.getPriority() == Task.Priority.HIGH
        ));
    }

    @Test
    @DisplayName("Should delete task successfully")
    void deleteTask_Success() {
        when(taskRepository.findByIdAndOwner(1L, sampleUser)).thenReturn(Optional.of(sampleTask));

        taskService.deleteTask(1L, sampleUser);

        verify(taskRepository).delete(sampleTask);
    }

    @Test
    @DisplayName("Should apply AI analysis to task")
    void applyAiAnalysis_Success() {
        TaskDTO.AnalysisResponse analysis = TaskDTO.AnalysisResponse.builder()
                .taskId(1L)
                .summary("AI generated summary")
                .suggestedPriority(Task.Priority.HIGH)
                .suggestedCategory(Task.Category.WORK)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        taskService.applyAiAnalysis(1L, analysis);

        verify(taskRepository).save(argThat(task ->
                task.getAiAnalysed() &&
                task.getAiSummary().equals("AI generated summary")
        ));
    }
}
