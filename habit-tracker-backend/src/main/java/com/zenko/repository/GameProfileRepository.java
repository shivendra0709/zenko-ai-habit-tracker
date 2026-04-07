package com.zenko.repository;

import com.zenko.model.GameProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GameProfileRepository extends JpaRepository<GameProfile, Long> {
    Optional<GameProfile> findByUserEmail(String email);
}
