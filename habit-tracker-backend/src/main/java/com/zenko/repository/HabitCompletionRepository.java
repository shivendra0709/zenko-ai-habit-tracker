package com.zenko.repository;

import com.zenko.model.HabitCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, Long> {

    List<HabitCompletion> findByUserId(Long userId);

    Optional<HabitCompletion> findByHabitIdAndCompletedDate(Long habitId, LocalDate date);
    Optional<HabitCompletion> findByHabitIdAndUserIdAndCompletedDate(Long habitId, Long userId, LocalDate date);

    @Query("SELECT hc FROM HabitCompletion hc WHERE hc.userId = :userId AND hc.completedDate >= :from")
    List<HabitCompletion> findByUserIdSince(Long userId, LocalDate from);

    void deleteByHabitId(Long habitId);
}
