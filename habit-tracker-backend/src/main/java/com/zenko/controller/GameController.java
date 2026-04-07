package com.zenko.controller;

import com.zenko.model.GameProfile;
import com.zenko.repository.GameProfileRepository;
import com.zenko.service.OpenRouterService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired private GameProfileRepository repo;
    @Autowired private OpenRouterService openRouter;

    private String getEmail(HttpSession s) { return (String) s.getAttribute("userEmail"); }

    private GameProfile getOrCreate(String email) {
        return repo.findByUserEmail(email).orElseGet(() -> repo.save(new GameProfile(email)));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        GameProfile p = getOrCreate(email);
        Map<String, Object> resp = new HashMap<>();
        resp.put("xp", p.getXp());
        resp.put("level", p.getLevel());
        resp.put("levelProgress", p.getLevelProgress());
        resp.put("xpForNextLevel", p.getXpForNextLevel());
        resp.put("streakTokens", p.getStreakTokens());
        resp.put("questsCompleted", p.getQuestsCompleted());
        resp.put("healthXp", p.getHealthXp());
        resp.put("mindXp", p.getMindXp());
        resp.put("wealthXp", p.getWealthXp());
        resp.put("healthLevel", p.getHealthLevel());
        resp.put("mindLevel", p.getMindLevel());
        resp.put("wealthLevel", p.getWealthLevel());
        resp.put("activeQuests", p.getActiveQuestsJson());
        resp.put("completedQuestIds", p.getCompletedQuestIds());
        resp.put("aiConfigured", openRouter.isConfigured());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/add-xp")
    public ResponseEntity<?> addXp(HttpSession session, @RequestBody Map<String, Object> body) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        GameProfile p = getOrCreate(email);
        int amount = (int) body.getOrDefault("amount", 10);
        String category = (String) body.getOrDefault("category", "");
        p.setXp(p.getXp() + amount);
        if ("Health".equalsIgnoreCase(category)) p.setHealthXp(p.getHealthXp() + amount);
        else if ("Mind".equalsIgnoreCase(category)) p.setMindXp(p.getMindXp() + amount);
        else if ("Wealth".equalsIgnoreCase(category)) p.setWealthXp(p.getWealthXp() + amount);
        repo.save(p);
        return ResponseEntity.ok(Map.of("xp", p.getXp(), "level", p.getLevel(), "levelProgress", p.getLevelProgress()));
    }

    @PostMapping("/use-token")
    public ResponseEntity<?> useToken(HttpSession session) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        GameProfile p = getOrCreate(email);
        if (p.getStreakTokens() <= 0) return ResponseEntity.badRequest().body(Map.of("error", "No freeze tokens left"));
        p.setStreakTokens(p.getStreakTokens() - 1);
        repo.save(p);
        return ResponseEntity.ok(Map.of("streakTokens", p.getStreakTokens()));
    }

    @PostMapping("/earn-token")
    public ResponseEntity<?> earnToken(HttpSession session) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        GameProfile p = getOrCreate(email);
        if (p.getStreakTokens() >= 5) return ResponseEntity.badRequest().body(Map.of("error", "Token limit reached"));
        p.setStreakTokens(p.getStreakTokens() + 1);
        repo.save(p);
        return ResponseEntity.ok(Map.of("streakTokens", p.getStreakTokens()));
    }

    @GetMapping("/quests")
    public ResponseEntity<?> getQuests(HttpSession session, @RequestParam(defaultValue = "") String habitsSummary) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        GameProfile p = getOrCreate(email);
        String quests = openRouter.generateQuests(habitsSummary.isEmpty() ? "general habits" : habitsSummary);
        p.setActiveQuestsJson(quests);
        repo.save(p);
        return ResponseEntity.ok(Map.of("quests", quests));
    }

    @PostMapping("/complete-quest/{questId}")
    public ResponseEntity<?> completeQuest(HttpSession session, @PathVariable int questId,
                                           @RequestBody(required=false) Map<String, Object> body) {
        String email = getEmail(session);
        if (email == null) return ResponseEntity.status(401).build();
        GameProfile p = getOrCreate(email);
        int xpReward = body != null ? (int) body.getOrDefault("xpReward", 100) : 100;
        p.setXp(p.getXp() + xpReward);
        p.setQuestsCompleted(p.getQuestsCompleted() + 1);
        String ids = p.getCompletedQuestIds();
        p.setCompletedQuestIds(ids.isEmpty() ? String.valueOf(questId) : ids + "," + questId);
        repo.save(p);
        return ResponseEntity.ok(Map.of("xp", p.getXp(), "level", p.getLevel(), "questsCompleted", p.getQuestsCompleted()));
    }
}
