package com.zenko.controller;

import com.zenko.model.Habit;
import com.zenko.model.HabitCompletion;
import com.zenko.repository.HabitCompletionRepository;
import com.zenko.repository.HabitRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class HabitController {

    @Autowired private HabitRepository habitRepo;
    @Autowired private HabitCompletionRepository completionRepo;

    // ── Resolve userId from session ──────────────────────────────
    private Long userId(HttpSession s) { return (Long) s.getAttribute("userId"); }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }

    // ── GET /api/habits ──────────────────────────────────────────
    @GetMapping("/habits")
    public ResponseEntity<?> getHabits(HttpSession session) {
        Long uid = userId(session);
        if (uid == null) return unauthorized();
        return ResponseEntity.ok(habitRepo.findByUserIdOrderByCreatedAtAsc(uid));
    }

    // ── POST /api/habits ─────────────────────────────────────────
    @PostMapping("/habits")
    public ResponseEntity<?> createHabit(@RequestBody Habit habit, HttpSession session) {
        Long uid = userId(session);
        if (uid == null) return unauthorized();
        habit.setId(null); // ensure it's a new entity
        habit.setUserId(uid);
        return ResponseEntity.ok(habitRepo.save(habit));
    }

    // ── PUT /api/habits/{id} ─────────────────────────────────────
    @PutMapping("/habits/{id}")
    public ResponseEntity<?> updateHabit(@PathVariable Long id, @RequestBody Habit updated, HttpSession session) {
        Long uid = userId(session);
        if (uid == null) return unauthorized();

        return habitRepo.findById(id)
            .filter(h -> h.getUserId().equals(uid))
            .map(h -> {
                h.setName(updated.getName());
                h.setEmoji(updated.getEmoji());
                h.setCategory(updated.getCategory());
                h.setFrequency(updated.getFrequency());
                h.setColor(updated.getColor());
                h.setTime(updated.getTime());
                h.setDescription(updated.getDescription());
                return ResponseEntity.ok(habitRepo.save(h));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE /api/habits/{id} ──────────────────────────────────
    @DeleteMapping("/habits/{id}")
    @Transactional
    public ResponseEntity<?> deleteHabit(@PathVariable Long id, HttpSession session) {
        Long uid = userId(session);
        if (uid == null) return unauthorized();

        Habit habit = habitRepo.findById(id).orElse(null);
        if (habit == null || !habit.getUserId().equals(uid)) {
            return ResponseEntity.notFound().build();
        }
        completionRepo.deleteByHabitId(id);
        habitRepo.delete(habit);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    // ── POST /api/habits/{id}/complete ───────────────────────────
    @PostMapping("/habits/{id}/complete")
    public ResponseEntity<?> toggleCompletion(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Long uid = userId(session);
        if (uid == null) return unauthorized();

        LocalDate date = LocalDate.parse(body.get("date")); // "YYYY-MM-DD"

        Optional<HabitCompletion> existing = completionRepo.findByHabitIdAndCompletedDate(id, date);
        if (existing.isPresent()) {
            completionRepo.delete(existing.get());
            return ResponseEntity.ok(Map.of("completed", false, "date", date.toString()));
        } else {
            HabitCompletion c = new HabitCompletion(id, uid, date);
            completionRepo.save(c);
            return ResponseEntity.ok(Map.of("completed", true, "date", date.toString()));
        }
    }

    // ── GET /api/completions ─────────────────────────────────────
    // Returns { habitId: { "YYYY-MM-DD": true } } map for the frontend
    @GetMapping("/completions")
    public ResponseEntity<?> getCompletions(HttpSession session) {
        Long uid = userId(session);
        if (uid == null) return unauthorized();

        LocalDate from = LocalDate.now().minusDays(365);
        List<HabitCompletion> completions = completionRepo.findByUserIdSince(uid, from);

        Map<String, Map<String, Boolean>> result = new HashMap<>();
        for (HabitCompletion c : completions) {
            String key = String.valueOf(c.getHabitId());
            result.computeIfAbsent(key, k -> new HashMap<>())
                  .put(c.getCompletedDate().toString(), true);
        }
        return ResponseEntity.ok(result);
    }
}
