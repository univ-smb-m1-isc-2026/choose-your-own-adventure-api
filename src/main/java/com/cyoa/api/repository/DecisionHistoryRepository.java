package com.cyoa.api.repository;

import com.cyoa.api.entity.DecisionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DecisionHistoryRepository extends JpaRepository<DecisionHistory, UUID> {
    List<DecisionHistory> findBySaveGameIdOrderByStepOrderAsc(UUID saveGameId);
    void deleteBySaveGameIdAndStepOrderGreaterThan(UUID saveGameId, Integer stepOrder);
    long countBySaveGameId(UUID saveGameId);
}
