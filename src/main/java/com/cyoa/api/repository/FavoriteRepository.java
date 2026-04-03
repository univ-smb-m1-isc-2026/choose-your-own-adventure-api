package com.cyoa.api.repository;

import com.cyoa.api.entity.Favorite;
import com.cyoa.api.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    List<Favorite> findByUserId(UUID userId);
    boolean existsByIdUserIdAndIdAdventureId(UUID userId, UUID adventureId);
    void deleteByIdUserIdAndIdAdventureId(UUID userId, UUID adventureId);
    long countByIdAdventureId(UUID adventureId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Favorite f where f.id.adventureId = :adventureId")
    void deleteByIdAdventureId(@Param("adventureId") UUID adventureId);
}
