package com.cyoa.api.repository;

import com.cyoa.api.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    List<Chapter> findByAdventureId(UUID adventureId);

    Optional<Chapter> findByAdventureIdAndIsStartTrue(UUID adventureId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Chapter c where c.adventure.id = :adventureId")
    void deleteByAdventureId(@Param("adventureId") UUID adventureId);
}
