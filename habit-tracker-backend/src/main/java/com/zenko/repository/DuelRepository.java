package com.zenko.repository;

import com.zenko.model.Duel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DuelRepository extends JpaRepository<Duel, Long> {
    List<Duel> findByChallenger_Id(Long userId);
    List<Duel> findByOpponent_Id(Long userId);
    List<Duel> findByChallenger_IdOrOpponent_Id(Long userId1, Long userId2);
    Optional<Duel> findByInviteCode(String inviteCode);
}
