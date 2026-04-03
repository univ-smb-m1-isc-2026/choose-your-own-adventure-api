package com.cyoa.api.repository;

import com.cyoa.api.entity.Effect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EffectRepository extends JpaRepository<Effect, UUID> {
    List<Effect> findByChapterId(UUID chapterId);
    List<Effect> findByChoiceId(UUID choiceId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Effect e where e.chapter.id = :chapterId")
    void deleteByChapterId(@Param("chapterId") UUID chapterId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Effect e where e.choice.id = :choiceId")
    void deleteByChoiceId(@Param("choiceId") UUID choiceId);
}
