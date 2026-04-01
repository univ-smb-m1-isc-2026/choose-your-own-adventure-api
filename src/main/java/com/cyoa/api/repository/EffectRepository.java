package com.cyoa.api.repository;

import com.cyoa.api.entity.Effect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EffectRepository extends JpaRepository<Effect, UUID> {
    List<Effect> findByChapterId(UUID chapterId);
    List<Effect> findByChoiceId(UUID choiceId);
}
