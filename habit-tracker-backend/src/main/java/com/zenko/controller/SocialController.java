package com.zenko.controller;

import com.zenko.model.AppUser;
import com.zenko.model.Duel;
import com.zenko.model.Bond;
import com.zenko.repository.DuelRepository;
import com.zenko.repository.BondRepository;
import com.zenko.service.OpenRouterService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    @Autowired private OpenRouterService openRouter;
    @Autowired private DuelRepository duelRepo;
    @Autowired private BondRepository bondRepo;
    @Autowired private com.zenko.repository.AppUserRepository userRepo;

    // Cities for anonymous feed
    private static final String[] CITIES = {
        "Tokyo", "London", "New York", "Paris", "Sydney", "Toronto", "Berlin",
        "Mumbai", "Seoul", "Dubai", "Barcelona", "Singapore", "São Paulo"
    };
    private static final String[] HABITS = {
        "meditation", "morning run", "cold shower", "journaling", "reading",
        "gym workout", "no-sugar day", "gratitude practice", "coding practice", "yoga"
    };

    private Long getUserId(HttpSession s) { return (Long) s.getAttribute("userId"); }
    private String getEmail(HttpSession s) { return (String) s.getAttribute("userEmail"); }
    private String now() { return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); }

    // ── Anonymous Feed ──
    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(HttpSession session) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        List<Map<String,Object>> feed = new ArrayList<>(generateSimulatedEvents(8));
        feed.sort((a, b) -> b.get("time").toString().compareTo(a.get("time").toString()));
        return ResponseEntity.ok(feed.subList(0, Math.min(20, feed.size())));
    }

    @PostMapping("/feed/post")
    public ResponseEntity<?> postToFeed(HttpSession session, @RequestBody Map<String,Object> body) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        Map<String,Object> event = new HashMap<>();
        event.put("id", UUID.randomUUID().toString());
        event.put("city", "Your City");
        event.put("habit", body.getOrDefault("habitName", "a habit"));
        event.put("streak", body.getOrDefault("streak", 1));
        event.put("emoji", body.getOrDefault("emoji", "✅"));
        event.put("time", now());
        event.put("isReal", true);
        return ResponseEntity.ok(event);
    }

    // ── Habit Duels ──
    @GetMapping("/duels")
    public ResponseEntity<?> getDuels(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        List<Duel> duels = duelRepo.findByChallenger_IdOrOpponent_Id(userId, userId);
        List<Map<String,Object>> result = duels.stream().map(this::duelToMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/duels")
    public ResponseEntity<?> createDuel(HttpSession session, @RequestBody Map<String,Object> body) {
        Long userId = getUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Get challenger user
        AppUser challenger = userRepo.getReferenceById(userId);

        Duel duel = new Duel();
        duel.setInviteCode(code);
        duel.setHabitName((String) body.getOrDefault("habitName", "My Habit"));
        duel.setChallenger(challenger);
        duel.setChallengerName((String) body.getOrDefault("challengerName", challenger.getName()));
        duel.setDaysTotal(30);
        duel.setChallengerScore(0);
        duel.setOpponentScore(0);
        duel.setStatus("pending");
        duel.setStartDate(LocalDateTime.now());

        duel = duelRepo.save(duel);
        return ResponseEntity.ok(duelToMap(duel));
    }

    @PostMapping("/duels/{code}/join")
    public ResponseEntity<?> joinDuel(HttpSession session, @PathVariable String code) {
        Long userId = getUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        Duel duel = duelRepo.findByInviteCode(code).orElse(null);
        if (duel == null || !"pending".equals(duel.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Duel not found or already started"));
        }

        AppUser opponent = userRepo.getReferenceById(userId);
        duel.setOpponent(opponent);
        duel.setOpponentName(opponent.getName());
        duel.setOpponentEmail(opponent.getEmail());
        duel.setStatus("active");
        duel = duelRepo.save(duel);

        return ResponseEntity.ok(duelToMap(duel));
    }

    // ── Accountability Bonds ──
    @GetMapping("/bonds")
    public ResponseEntity<?> getBonds(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        List<Bond> bonds = bondRepo.findByUser1_IdOrUser2_Id(userId, userId);
        List<Map<String,Object>> result = bonds.stream().map(this::bondToMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bonds")
    public ResponseEntity<?> createBond(HttpSession session, @RequestBody Map<String,Object> body) {
        Long userId = getUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Get user1
        AppUser user1 = userRepo.getReferenceById(userId);

        Bond bond = new Bond();
        bond.setInviteCode(code);
        bond.setHabitName((String) body.getOrDefault("habitName", "Shared Habit"));
        bond.setUser1(user1);
        bond.setUser1Name((String) body.getOrDefault("user1Name", user1.getName()));
        bond.setUser1Streak(0);
        bond.setUser2Streak(0);
        bond.setSharedStreak(0);
        bond.setStatus("pending");

        bond = bondRepo.save(bond);
        return ResponseEntity.ok(bondToMap(bond));
    }

    @PostMapping("/bonds/{code}/join")
    public ResponseEntity<?> joinBond(HttpSession session, @PathVariable String code) {
        Long userId = getUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        Bond bond = bondRepo.findByInviteCode(code).orElse(null);
        if (bond == null || !"pending".equals(bond.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bond not found or already joined"));
        }

        AppUser user2 = userRepo.getReferenceById(userId);
        bond.setUser2(user2);
        bond.setUserName2(user2.getName());
        bond.setUser2Email(user2.getEmail());
        bond.setStatus("active");
        bondRepo.save(bond);

        return ResponseEntity.ok(bondToMap(bond));
    }

    // ── AI endpoints ──
    @PostMapping("/ai/autopsy")
    @RateLimiter(name = "aiEndpoints")
    public ResponseEntity<?> habitAutopsy(HttpSession session, @RequestBody Map<String,Object> body) {
        if (getEmail(session) == null) return ResponseEntity.status(401).build();
        String result = openRouter.habitAutopsy(
            (String) body.getOrDefault("habitName", "Unknown Habit"),
            (int) body.getOrDefault("streak", 0),
            (String) body.get("reason"));
        return ResponseEntity.ok(Map.of("report", result, "aiGenerated", openRouter.isConfigured()));
    }

    @PostMapping("/ai/dna")
    @RateLimiter(name = "aiEndpoints")
    public ResponseEntity<?> habitDNA(HttpSession session, @RequestBody Map<String,Object> body) {
        if (getEmail(session) == null) return ResponseEntity.status(401).build();
        String result = openRouter.habitDNA(
            (String) body.getOrDefault("summary", ""),
            (String) body.getOrDefault("userName", "User"));
        return ResponseEntity.ok(Map.of("profile", result, "aiGenerated", openRouter.isConfigured()));
    }

    @PostMapping("/ai/alert")
    @RateLimiter(name = "aiEndpoints")
    public ResponseEntity<?> predictiveAlert(HttpSession session, @RequestBody Map<String,Object> body) {
        if (getEmail(session) == null) return ResponseEntity.status(401).build();
        String result = openRouter.predictiveAlert(
            (String) body.getOrDefault("habitName", "habit"),
            (int) body.getOrDefault("missRate", 30),
            (String) body.getOrDefault("timePattern", "morning"));
        return ResponseEntity.ok(Map.of("alert", result, "aiGenerated", openRouter.isConfigured()));
    }

    @PostMapping("/ai/insights")
    @RateLimiter(name = "aiEndpoints")
    public ResponseEntity<?> aiInsights(HttpSession session, @RequestBody Map<String,Object> body) {
        if (getEmail(session) == null) return ResponseEntity.status(401).build();
        String result = openRouter.generateInsights(
            (String) body.getOrDefault("summary", ""),
            (String) body.getOrDefault("userName", "User"));
        return ResponseEntity.ok(Map.of("insights", result, "aiGenerated", openRouter.isConfigured()));
    }

    @PostMapping("/ai/chat")
    @RateLimiter(name = "aiEndpoints")
    public ResponseEntity<?> aiChat(HttpSession session, @RequestBody Map<String,Object> body) {
        if (getEmail(session) == null) return ResponseEntity.status(401).build();
        String message    = (String) body.getOrDefault("message", "");
        String habitCtx   = (String) body.getOrDefault("habitContext", "");
        String userName   = (String) body.getOrDefault("userName", "User");
        String reply = openRouter.chatReply(message, habitCtx, userName);
        return ResponseEntity.ok(Map.of("reply", reply, "aiGenerated", openRouter.isConfigured()));
    }

    // ── Converters ──
    private Map<String,Object> duelToMap(Duel d) {
        Map<String,Object> map = new HashMap<>();
        map.put("id", d.getId());
        map.put("inviteCode", d.getInviteCode());
        map.put("habitName", d.getHabitName());
        map.put("challengerEmail", d.getChallenger() != null ? d.getChallenger().getEmail() : null);
        map.put("challengerName", d.getChallengerName());
        map.put("opponentEmail", d.getOpponentEmail());
        map.put("opponentName", d.getOpponentName());
        map.put("daysTotal", d.getDaysTotal());
        map.put("challengerScore", d.getChallengerScore());
        map.put("opponentScore", d.getOpponentScore());
        map.put("status", d.getStatus());
        map.put("startDate", d.getStartDate() != null ? d.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return map;
    }

    private Map<String,Object> bondToMap(Bond b) {
        Map<String,Object> map = new HashMap<>();
        map.put("id", b.getId());
        map.put("inviteCode", b.getInviteCode());
        map.put("habitName", b.getHabitName());
        map.put("user1Email", b.getUser1() != null ? b.getUser1().getEmail() : null);
        map.put("user1Name", b.getUser1Name());
        map.put("user2Email", b.getUser2Email());
        map.put("user2Name", b.getUserName2());
        map.put("user1Streak", b.getUser1Streak());
        map.put("user2Streak", b.getUser2Streak());
        map.put("sharedStreak", b.getSharedStreak());
        map.put("status", b.getStatus());
        map.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return map;
    }

    // ── Helpers ──
    private List<Map<String,Object>> generateSimulatedEvents(int count) {
        Random rnd = new Random();
        List<Map<String,Object>> events = new ArrayList<>();
        String now = now();
        for (int i = 0; i < count; i++) {
            Map<String,Object> e = new HashMap<>();
            e.put("id", UUID.randomUUID().toString());
            e.put("city", CITIES[rnd.nextInt(CITIES.length)]);
            String habit = HABITS[rnd.nextInt(HABITS.length)];
            e.put("habit", habit);
            int streak = rnd.nextInt(90) + 1;
            e.put("streak", streak);
            e.put("emoji", streak > 30 ? "🔥" : streak > 7 ? "⚡" : "✅");
            e.put("minutesAgo", rnd.nextInt(55) + 1);
            e.put("time", now);
            e.put("isReal", false);
            events.add(e);
        }
        return events;
    }
}
