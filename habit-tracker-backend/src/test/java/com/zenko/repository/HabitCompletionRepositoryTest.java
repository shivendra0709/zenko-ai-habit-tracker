package com.zenko.repository;

import com.zenko.model.Habit;
import com.zenko.model.HabitCompletion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class HabitCompletionRepositoryTest {

    @Autowired
    private HabitCompletionRepository completionRepo;

    @Autowired
    private HabitRepository habitRepo;

    private Long testHabitId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Create a habit for testing
        Habit habit = new Habit();
        habit.setUserId(1L);
        habit.setName("Test Habit");
        habit.setCategory("health");
        habitRepo.save(habit);

        testHabitId = habit.getId();
        testUserId = habit.getUserId();
    }

    @Test
    void testSaveAndFindByHabitIdAndCompletedDate() {
        // Given
        HabitCompletion completion = new HabitCompletion(testHabitId, testUserId, LocalDate.now());
        completionRepo.save(completion);

        // When
        Optional<HabitCompletion> found = completionRepo.findByHabitIdAndCompletedDate(testHabitId, LocalDate.now());

        // Then
        assertTrue(found.isPresent());
        assertEquals(testHabitId, found.get().getHabitId());
        assertEquals(testUserId, found.get().getUserId());
    }

    @Test
    void testFindByHabitIdAndCompletedDate_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<HabitCompletion> result = completionRepo.findByHabitIdAndCompletedDate(testHabitId, LocalDate.now());

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByUserId_ShouldReturnAllUserCompletions() {
        // Given: create multiple completions for same user
        Habit habit1 = new Habit();
        habit1.setUserId(testUserId);
        habit1.setName("Habit 1");
        habitRepo.save(habit1);

        Habit habit2 = new Habit();
        habit2.setUserId(testUserId);
        habit2.setName("Habit 2");
        habitRepo.save(habit2);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        completionRepo.save(new HabitCompletion(habit1.getId(), testUserId, today));
        completionRepo.save(new HabitCompletion(habit2.getId(), testUserId, today));
        completionRepo.save(new HabitCompletion(habit1.getId(), testUserId, yesterday));

        // When
        List<HabitCompletion> userCompletions = completionRepo.findByUserId(testUserId);

        // Then
        assertEquals(3, userCompletions.size());
        assertTrue(userCompletions.stream().allMatch(c -> c.getUserId().equals(testUserId)));
    }

    @Test
    void testFindByUserIdSince_ShouldFilterByDate() {
        // Given
        Habit habit = new Habit();
        habit.setUserId(testUserId);
        habit.setName("Test");
        habitRepo.save(habit);

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate sixtyDaysAgo = today.minusDays(60);

        completionRepo.save(new HabitCompletion(habit.getId(), testUserId, today));
        completionRepo.save(new HabitCompletion(habit.getId(), testUserId, thirtyDaysAgo));
        completionRepo.save(new HabitCompletion(habit.getId(), testUserId, sixtyDaysAgo)); // too old

        // When
        List<HabitCompletion> recent = completionRepo.findByUserIdSince(testUserId, thirtyDaysAgo.minusDays(1));

        // Then: should include 30 days ago and today, but not 60 days ago
        assertEquals(2, recent.size());
        assertTrue(recent.stream().noneMatch(c -> c.getCompletedDate().isBefore(thirtyDaysAgo)));
    }

    @Test
    void testDeleteByHabitId_ShouldRemoveAllCompletionsForHabit() {
        // Given
        Habit habit = new Habit();
        habit.setUserId(testUserId);
        habit.setName("To Delete");
        habitRepo.save(habit);

        Long habitId = habit.getId();

        completionRepo.save(new HabitCompletion(habitId, testUserId, LocalDate.now()));
        completionRepo.save(new HabitCompletion(habitId, testUserId, LocalDate.now().minusDays(1)));
        completionRepo.save(new HabitCompletion(habitId, testUserId, LocalDate.now().minusDays(2)));

        assertEquals(3, completionRepo.findByUserId(testUserId).size());

        // When
        completionRepo.deleteByHabitId(habitId);

        // Then
        assertEquals(0, completionRepo.findByUserId(testUserId).size());
    }

    @Test
    void testDeleteByHabitId_ShouldOnlyDeleteForThatHabit() {
        // Given: two habits with completions
        Habit habit1 = new Habit();
        habit1.setUserId(testUserId);
        habit1.setName("Habit 1");
        habitRepo.save(habit1);

        Habit habit2 = new Habit();
        habit2.setUserId(testUserId);
        habit2.setName("Habit 2");
        habitRepo.save(habit2);

        completionRepo.save(new HabitCompletion(habit1.getId(), testUserId, LocalDate.now()));
        completionRepo.save(new HabitCompletion(habit2.getId(), testUserId, LocalDate.now()));

        // When: delete completions for habit2 only
        completionRepo.deleteByHabitId(habit2.getId());

        // Then
        List<HabitCompletion> remaining = completionRepo.findByUserId(testUserId);
        assertEquals(1, remaining.size());
        assertEquals(habit1.getId(), remaining.get(0).getHabitId());
    }

    @Test
    void testFindByHabitIdAndCompletedDate_ReturnsMostRecentOrFirst() {
        // Given: completion exists for today
        HabitCompletion existing = new HabitCompletion(testHabitId, testUserId, LocalDate.now());
        completionRepo.save(existing);

        // When: query forToday's completion
        Optional<HabitCompletion> found = completionRepo.findByHabitIdAndCompletedDate(testHabitId, LocalDate.now());

        // Then: should find a completion
        assertTrue(found.isPresent());
        assertEquals(testHabitId, found.get().getHabitId());
        assertEquals(testUserId, found.get().getUserId());
    }
}
