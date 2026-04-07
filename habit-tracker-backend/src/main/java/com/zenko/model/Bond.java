package com.zenko.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bonds")
public class Bond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private String habitName;

    // User 1 (creator)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private AppUser user1;

    @Column(name = "user1_name", nullable = false)
    private String user1Name;

    // User 2 (partner)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private AppUser user2;

    @Column(name = "user2_name")
    private String userName2;

    @Column(name = "user2_email")
    private String user2Email;

    @Column(name = "user1_streak", nullable = false)
    private Integer user1Streak = 0;

    @Column(name = "user2_streak", nullable = false)
    private Integer user2Streak = 0;

    @Column(name = "shared_streak", nullable = false)
    private Integer sharedStreak = 0;

    @Column(nullable = false)
    private String status = "pending"; // pending, active

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getHabitName() { return habitName; }
    public void setHabitName(String habitName) { this.habitName = habitName; }
    public AppUser getUser1() { return user1; }
    public void setUser1(AppUser user1) { this.user1 = user1; }
    public String getUser1Name() { return user1Name; }
    public void setUser1Name(String userName1) { this.user1Name = userName1; }
    public AppUser getUser2() { return user2; }
    public void setUser2(AppUser user2) { this.user2 = user2; }
    public String getUserName2() { return userName2; }
    public void setUserName2(String userName2) { this.userName2 = userName2; }
    public String getUser2Email() { return user2Email; }
    public void setUser2Email(String user2Email) { this.user2Email = user2Email; }
    public Integer getUser1Streak() { return user1Streak; }
    public void setUser1Streak(Integer user1Streak) { this.user1Streak = user1Streak; }
    public Integer getUser2Streak() { return user2Streak; }
    public void setUser2Streak(Integer user2Streak) { this.user2Streak = user2Streak; }
    public Integer getSharedStreak() { return sharedStreak; }
    public void setSharedStreak(Integer sharedStreak) { this.sharedStreak = sharedStreak; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
