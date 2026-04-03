package com.cyoa.api.repository;

import com.cyoa.api.entity.SaveGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaveGameRepository extends JpaRepository<SaveGame, UUID> {
    List<SaveGame> findByUserId(UUID userId);

    List<SaveGame> findByUserIdAndAdventureIdOrderByLastPlayedDesc(UUID userId, UUID adventureId);

    List<SaveGame> findByAdventureId(UUID adventureId);

    @Modifying(clearAutomatically = true)
    @Query("delete from SaveGame s where s.adventure.id = :adventureId")
    void deleteByAdventureId(@Param("adventureId") UUID adventureId);
}
