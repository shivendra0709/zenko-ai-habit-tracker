package com.zenko.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "duels")
public class Duel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private String habitName;

    // Challenger (user who created the duel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenger_id", nullable = false)
    private AppUser challenger;

    @Column(name = "challenger_name", nullable = false)
    private String challengerName;

    // Opponent (user who joins)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_id")
    private AppUser opponent;

    @Column(name = "opponent_name")
    private String opponentName;

    @Column(name = "opponent_email")
    private String opponentEmail;

    @Column(nullable = false)
    private Integer daysTotal = 30;

    @Column(name = "challenger_score", nullable = false)
    private Integer challengerScore = 0;

    @Column(name = "opponent_score", nullable = false)
    private Integer opponentScore = 0;

    @Column(nullable = false)
    private String status = "pending"; // pending, active, completed

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "habit_id")
    private Long habitId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) startDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getHabitName() { return habitName; }
    public void setHabitName(String habitName) { this.habitName = habitName; }
    public AppUser getChallenger() { return challenger; }
    public void setChallenger(AppUser challenger) { this.challenger = challenger; }
    public String getChallengerName() { return challengerName; }
    public void setChallengerName(String challengerName) { this.challengerName = challengerName; }
    public AppUser getOpponent() { return opponent; }
    public void setOpponent(AppUser opponent) { this.opponent = opponent; }
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public String getOpponentEmail() { return opponentEmail; }
    public void setOpponentEmail(String opponentEmail) { this.opponentEmail = opponentEmail; }
    public Integer getDaysTotal() { return daysTotal; }
    public void setDaysTotal(Integer daysTotal) { this.daysTotal = daysTotal; }
    public Integer getChallengerScore() { return challengerScore; }
    public void setChallengerScore(Integer challengerScore) { this.challengerScore = challengerScore; }
    public Integer getOpponentScore() { return opponentScore; }
    public void setOpponentScore(Integer opponentScore) { this.opponentScore = opponentScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getHabitId() { return habitId; }
    public void setHabitId(Long habitId) { this.habitId = habitId; }
}
