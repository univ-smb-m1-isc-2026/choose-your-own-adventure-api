package com.cyoa.api.repository;

import com.cyoa.api.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByAdventureId(UUID adventureId);
    Optional<Item> findByAdventureIdAndName(UUID adventureId, String name);

    @Modifying(clearAutomatically = true)
    @Query("delete from Item i where i.adventure.id = :adventureId")
    void deleteByAdventureId(@Param("adventureId") UUID adventureId);
}
