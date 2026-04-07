package com.zenko.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_profiles")
public class GameProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userEmail;

    private int xp = 0;
    private int streakTokens = 3;
    private int questsCompleted = 0;

    // Skill tree XP per category
    private int healthXp = 0;
    private int mindXp = 0;
    private int wealthXp = 0;

    // Active quests (JSON stored as string)
    @Column(length = 2000)
    private String activeQuestsJson = "[]";

    // Completed quests tracking
    @Column(length = 1000)
    private String completedQuestIds = "";

    public GameProfile() {}
    public GameProfile(String userEmail) { this.userEmail = userEmail; }

    // ── Computed ──
    public int getLevel() {
        if (xp < 100) return 1;
        if (xp < 300) return 2;
        if (xp < 600) return 3;
        if (xp < 1000) return 4;
        if (xp < 1500) return 5;
        if (xp < 2500) return 6;
        if (xp < 4000) return 7;
        if (xp < 6000) return 8;
        if (xp < 9000) return 9;
        return 10;
    }

    public int getXpForNextLevel() {
        int[] thresholds = {100, 300, 600, 1000, 1500, 2500, 4000, 6000, 9000, Integer.MAX_VALUE};
        return thresholds[Math.min(getLevel() - 1, 9)];
    }

    public int getXpForCurrentLevel() {
        int[] thresholds = {0, 100, 300, 600, 1000, 1500, 2500, 4000, 6000, 9000};
        return thresholds[Math.min(getLevel() - 1, 9)];
    }

    public int getLevelProgress() {
        int curr = getXpForCurrentLevel();
        int next = getXpForNextLevel();
        if (next == Integer.MAX_VALUE) return 100;
        return (int) ((double)(xp - curr) / (next - curr) * 100);
    }

    public int getHealthLevel() { return healthXp < 50 ? 1 : healthXp < 150 ? 2 : healthXp < 350 ? 3 : healthXp < 700 ? 4 : 5; }
    public int getMindLevel()   { return mindXp   < 50 ? 1 : mindXp   < 150 ? 2 : mindXp   < 350 ? 3 : mindXp   < 700 ? 4 : 5; }
    public int getWealthLevel() { return wealthXp < 50 ? 1 : wealthXp < 150 ? 2 : wealthXp < 350 ? 3 : wealthXp < 700 ? 4 : 5; }

    // ── Getters / Setters ──
    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String e) { userEmail = e; }
    public int getXp() { return xp; }
    public void setXp(int x) { xp = x; }
    public int getStreakTokens() { return streakTokens; }
    public void setStreakTokens(int t) { streakTokens = t; }
    public int getQuestsCompleted() { return questsCompleted; }
    public void setQuestsCompleted(int q) { questsCompleted = q; }
    public int getHealthXp() { return healthXp; }
    public void setHealthXp(int x) { healthXp = x; }
    public int getMindXp() { return mindXp; }
    public void setMindXp(int x) { mindXp = x; }
    public int getWealthXp() { return wealthXp; }
    public void setWealthXp(int x) { wealthXp = x; }
    public String getActiveQuestsJson() { return activeQuestsJson; }
    public void setActiveQuestsJson(String j) { activeQuestsJson = j; }
    public String getCompletedQuestIds() { return completedQuestIds; }
    public void setCompletedQuestIds(String c) { completedQuestIds = c; }
}
