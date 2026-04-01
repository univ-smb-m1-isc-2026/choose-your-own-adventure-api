package com.cyoa.api.repository;

import com.cyoa.api.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, UUID> {
    List<Condition> findByChapterId(UUID chapterId);
    List<Condition> findByChoiceId(UUID choiceId);
}
