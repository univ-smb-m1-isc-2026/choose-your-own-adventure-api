package com.cyoa.api.repository;

import com.cyoa.api.entity.Adventure;
import com.cyoa.api.entity.enums.AdventureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdventureRepository extends JpaRepository<Adventure, UUID> {
    List<Adventure> findByStatus(AdventureStatus status);

    List<Adventure> findByAuthorId(UUID authorId);
}
