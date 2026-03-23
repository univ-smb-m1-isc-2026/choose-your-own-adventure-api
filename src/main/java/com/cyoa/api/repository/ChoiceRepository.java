package com.cyoa.api.repository;

import com.cyoa.api.entity.Choice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChoiceRepository extends JpaRepository<Choice, UUID> {
    List<Choice> findByFromChapterId(UUID chapterId);
}
