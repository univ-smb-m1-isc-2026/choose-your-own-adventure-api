package com.cyoa.api.repository;

import com.cyoa.api.entity.AdventureStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdventureStatsRepository extends JpaRepository<AdventureStats, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("delete from AdventureStats s where s.adventureId = :adventureId")
    void deleteByAdventureId(@Param("adventureId") UUID adventureId);
}
