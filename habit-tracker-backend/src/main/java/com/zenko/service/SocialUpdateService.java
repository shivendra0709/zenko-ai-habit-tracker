package com.zenko.service;

import com.zenko.model.Bond;
import com.zenko.model.HabitCompletion;
import com.zenko.repository.BondRepository;
import com.zenko.repository.DuelRepository;
import com.zenko.repository.HabitCompletionRepository;
import com.zenko.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SocialUpdateService {

    @Autowired private DuelRepository duelRepo;
    @Autowired private BondRepository bondRepo;
    @Autowired private HabitRepository habitRepo;
    @Autowired private HabitCompletionRepository completionRepo;

    @Transactional
    public void onHabitCompleted(HabitCompletion completion) {
        Long habitId = completion.getHabitId();
        Long userId = completion.getUserId();
        LocalDate completedDate = completion.getCompletedDate();

        // Update active duels where this user is involved and habit matches
        // Find duels where user is challenger
        duelRepo.findByHabitIdAndChallenger_Id(habitId, userId).stream()
            .filter(d -> "active".equals(d.getStatus()))
            .forEach(d -> {
                d.setChallengerScore(d.getChallengerScore() + 1);
                duelRepo.save(d);
            });

        // Find duels where user is opponent
        duelRepo.findByHabitIdAndOpponent_Id(habitId, userId).stream()
            .filter(d -> "active".equals(d.getStatus()))
            .forEach(d -> {
                d.setOpponentScore(d.getOpponentScore() + 1);
                duelRepo.save(d);
            });

        // Update bonds where user is user1 or user2
        List<Bond> user1Bonds = bondRepo.findByHabitIdAndUser1_Id(habitId, userId);
        List<Bond> user2Bonds = bondRepo.findByHabitIdAndUser2_Id(habitId, userId);

        for (Bond bond : user1Bonds) {
            updateBondStreaks(bond, userId, true, completedDate);
        }
        for (Bond bond : user2Bonds) {
            updateBondStreaks(bond, userId, false, completedDate);
        }
    }

    private void updateBondStreaks(Bond bond, Long completingUserId, boolean isUser1, LocalDate completedDate) {
        // Increment individual streak
        if (isUser1) {
            bond.setUser1Streak(bond.getUser1Streak() + 1);
        } else {
            bond.setUser2Streak(bond.getUser2Streak() + 1);
        }

        // Check if the other user has also completed today
        Long otherUserId = isUser1
            ? (bond.getUser2() != null ? bond.getUser2().getId() : null)
            : (bond.getUser1() != null ? bond.getUser1().getId() : null);

        if (otherUserId != null) {
            Optional<HabitCompletion> otherCompletion = completionRepo
                .findByHabitIdAndUserIdAndCompletedDate(bond.getHabitId(), otherUserId, completedDate);

            if (otherCompletion.isPresent()) {
                // Both completed today! Increment shared streak
                bond.setSharedStreak(bond.getSharedStreak() + 1);
            } else {
                // If only one completed today, shared streak resets (since shared means both must complete)
                bond.setSharedStreak(0);
            }
        }

        bondRepo.save(bond);
    }
}
