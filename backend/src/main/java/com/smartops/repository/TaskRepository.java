package com.smartops.repository;

import com.smartops.model.Task;
import com.smartops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwner(User owner);

    List<Task> findByOwnerAndStatus(User owner, Task.Status status);

    List<Task> findByAiAnalysedFalse();

    Optional<Task> findByIdAndOwner(Long id, User owner);

    @Query("SELECT t FROM Task t WHERE t.status = 'DONE' AND t.updatedAt < :cutoff")
    List<Task> findOldCompletedTasks(LocalDateTime cutoff);
}
