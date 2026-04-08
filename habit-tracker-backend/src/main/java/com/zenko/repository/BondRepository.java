package com.zenko.repository;

import com.zenko.model.Bond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BondRepository extends JpaRepository<Bond, Long> {
    List<Bond> findByUser1_Id(Long userId);
    List<Bond> findByUser2_Id(Long userId);
    List<Bond> findByUser1_IdOrUser2_Id(Long userId1, Long userId2);
    Optional<Bond> findByInviteCode(String inviteCode);
    List<Bond> findByHabitIdAndStatus(Long habitId, String status);
    List<Bond> findByHabitIdAndUser1_Id(Long habitId, Long userId);
    List<Bond> findByHabitIdAndUser2_Id(Long habitId, Long userId);
}
