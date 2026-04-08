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
class HabitRepositoryTest {

    @Autowired
    private HabitRepository habitRepo;

    @Autowired
    private HabitCompletionRepository completionRepo;

    @Autowired
    private AppUserRepository userRepo;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Create a test user implicitly via Habit (userId field)
        testUserId = 1L; // Simulated user ID for test data
    }

    @Test
    void testSaveAndFindByUserId() {
        // Given
        Habit habit1 = new Habit();
        habit1.setUserId(testUserId);
        habit1.setName("Exercise");
        habit1.setCategory("health");
        habit1.setFrequency("daily");
        habitRepo.save(habit1);

        Habit habit2 = new Habit();
        habit2.setUserId(testUserId);
        habit2.setName("Reading");
        habit2.setCategory("mind");
        habit2.setFrequency("daily");
        habitRepo.save(habit2);

        // Habit for another user
        Habit otherHabit = new Habit();
        otherHabit.setUserId(999L);
        otherHabit.setName("Other");
        otherHabit.setCategory("wealth");
        habitRepo.save(otherHabit);

        // When
        List<Habit> userHabits = habitRepo.findByUserIdOrderByCreatedAtAsc(testUserId);

        // Then
        assertEquals(2, userHabits.size());
        assertTrue(userHabits.stream().allMatch(h -> h.getUserId().equals(testUserId)));
        assertEquals("Exercise", userHabits.get(0).getName()); // first created
        assertEquals("Reading", userHabits.get(1).getName());
    }

    @Test
    void testFindByUserIdOrderByCreatedAtAsc_ShouldReturnInOrder() {
        // Given: create habits with different created times
        Habit h1 = new Habit();
        h1.setUserId(testUserId);
        h1.setName("First");
        habitRepo.save(h1);

        Habit h2 = new Habit();
        h2.setUserId(testUserId);
        h2.setName("Second");
        habitRepo.save(h2);

        // When
        List<Habit> result = habitRepo.findByUserIdOrderByCreatedAtAsc(testUserId);

        // Then: should be ordered by createdAt ascending (oldest first)
        assertEquals("First", result.get(0).getName());
        assertEquals("Second", result.get(1).getName());
    }

    @Test
    void testDeleteByIdAndUserId_ShouldOnlyDeleteOwnHabit() {
        // Given
        Habit myHabit = new Habit();
        myHabit.setUserId(testUserId);
        myHabit.setName("My Habit");
        habitRepo.save(myHabit);

        Habit otherHabit = new Habit();
        otherHabit.setUserId(999L);
        otherHabit.setName("Other Habit");
        habitRepo.save(otherHabit);

        // When: delete only my habit
        habitRepo.deleteByIdAndUserId(myHabit.getId(), testUserId);

        // Then
        assertTrue(habitRepo.findById(myHabit.getId()).isEmpty());
        assertTrue(habitRepo.findById(otherHabit.getId()).isPresent()); // other habit still exists
    }

    @Test
    void testDeleteByIdAndUserId_WhenWrongUser_ShouldNotDelete() {
        // Given
        Habit myHabit = new Habit();
        myHabit.setUserId(testUserId);
        myHabit.setName("My Habit");
        habitRepo.save(myHabit);

        // When: try to delete with wrong userId
        habitRepo.deleteByIdAndUserId(myHabit.getId(), 999L);

        // Then: habit should still exist
        assertTrue(habitRepo.findById(myHabit.getId()).isPresent());
    }
}
