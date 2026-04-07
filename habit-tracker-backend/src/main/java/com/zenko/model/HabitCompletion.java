package com.zenko.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "habit_completions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"habitId", "completedDate"}))
public class HabitCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long habitId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate completedDate;

    public HabitCompletion() {}

    public HabitCompletion(Long habitId, Long userId, LocalDate completedDate) {
        this.habitId = habitId;
        this.userId = userId;
        this.completedDate = completedDate;
    }

    public Long getId() { return id; }
    public Long getHabitId() { return habitId; }
    public void setHabitId(Long habitId) { this.habitId = habitId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
}
