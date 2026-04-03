package com.cyoa.api.repository;

import com.cyoa.api.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findBySaveGameId(UUID saveGameId);
    Optional<InventoryItem> findBySaveGameIdAndItemId(UUID saveGameId, UUID itemId);

    @Modifying(clearAutomatically = true)
    @Query("delete from InventoryItem i where i.saveGame.id = :saveGameId")
    void deleteBySaveGameId(@Param("saveGameId") UUID saveGameId);
}
