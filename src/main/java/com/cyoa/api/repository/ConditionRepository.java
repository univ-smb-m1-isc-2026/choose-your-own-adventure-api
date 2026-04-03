package com.cyoa.api.repository;

import com.cyoa.api.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, UUID> {
    List<Condition> findByChapterId(UUID chapterId);
    List<Condition> findByChoiceId(UUID choiceId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Condition c where c.chapter.id = :chapterId")
    void deleteByChapterId(@Param("chapterId") UUID chapterId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Condition c where c.choice.id = :choiceId")
    void deleteByChoiceId(@Param("choiceId") UUID choiceId);
}
