package com.cyoa.api.repository;

import com.cyoa.api.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findBySaveGameId(UUID saveGameId);
    Optional<InventoryItem> findBySaveGameIdAndItemId(UUID saveGameId, UUID itemId);
}
