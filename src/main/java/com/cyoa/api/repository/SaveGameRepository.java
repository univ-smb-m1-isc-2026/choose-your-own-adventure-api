package com.cyoa.api.repository;

import com.cyoa.api.entity.SaveGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaveGameRepository extends JpaRepository<SaveGame, UUID> {
    List<SaveGame> findByUserId(UUID userId);

    Optional<SaveGame> findByUserIdAndAdventureId(UUID userId, UUID adventureId);
}
