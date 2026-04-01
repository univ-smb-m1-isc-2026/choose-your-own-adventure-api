package com.cyoa.api.repository;

import com.cyoa.api.entity.AdventureStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdventureStatsRepository extends JpaRepository<AdventureStats, UUID> {
}
