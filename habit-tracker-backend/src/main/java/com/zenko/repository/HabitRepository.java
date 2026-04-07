package com.zenko.repository;

import com.zenko.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUserIdOrderByCreatedAtAsc(Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
