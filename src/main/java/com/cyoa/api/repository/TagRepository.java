package com.cyoa.api.repository;

import com.cyoa.api.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByAdventureId(UUID adventureId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Tag t where t.adventure.id = :adventureId")
    void deleteByAdventureId(@Param("adventureId") UUID adventureId);
}
