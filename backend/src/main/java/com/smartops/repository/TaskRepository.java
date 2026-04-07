package com.smartops.repository;

import com.smartops.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(Task.Status status);

    List<Task> findByPriority(Task.Priority priority);

    List<Task> findByAiAnalysedFalse();

    @Query("SELECT t FROM Task t WHERE t.status = 'DONE' AND t.updatedAt < :cutoff")
    List<Task> findOldCompletedTasks(LocalDateTime cutoff);

    List<Task> findByStatusAndPriority(Task.Status status, Task.Priority priority);
}
