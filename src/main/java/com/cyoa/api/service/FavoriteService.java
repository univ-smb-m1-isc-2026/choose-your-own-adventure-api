package com.cyoa.api.service;

import com.cyoa.api.dto.response.AdventureSummaryResponse;
import com.cyoa.api.entity.*;
import com.cyoa.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AdventureRepository adventureRepository;
    private final TagRepository tagRepository;
    private final ChapterRepository chapterRepository;
    private final AdventureStatsRepository statsRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           AdventureRepository adventureRepository,
                           TagRepository tagRepository,
                           ChapterRepository chapterRepository,
                           AdventureStatsRepository statsRepository) {
        this.favoriteRepository = favoriteRepository;
        this.adventureRepository = adventureRepository;
        this.tagRepository = tagRepository;
        this.chapterRepository = chapterRepository;
        this.statsRepository = statsRepository;
    }

    @Transactional
    public boolean toggle(UUID userId, UUID adventureId) {
        boolean exists = favoriteRepository.existsByIdUserIdAndIdAdventureId(userId, adventureId);
        if (exists) {
            favoriteRepository.deleteByIdUserIdAndIdAdventureId(userId, adventureId);
            return false;
        } else {
            Adventure adventure = adventureRepository.findById(adventureId)
                    .orElseThrow(() -> new RuntimeException("Adventure not found"));
            Favorite fav = Favorite.builder()
                    .id(new FavoriteId(userId, adventureId))
                    .build();
            favoriteRepository.save(fav);
            return true;
        }
    }

    public List<AdventureSummaryResponse> getFavorites(UUID userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(f -> {
                    Adventure a = adventureRepository.findById(f.getId().getAdventureId()).orElse(null);
                    if (a == null) return null;
                    List<Tag> tags = tagRepository.findByAdventureId(a.getId());
                    List<Chapter> chapters = chapterRepository.findByAdventureId(a.getId());
                    int totalReads = statsRepository.findById(a.getId()).map(AdventureStats::getTotalReads).orElse(0);

                    return AdventureSummaryResponse.builder()
                            .id(a.getId())
                            .title(a.getTitle())
                            .summary(a.getSummary())
                            .difficulty(a.getDifficulty())
                            .language(a.getLanguage())
                            .status(a.getStatus())
                            .authorUsername(a.getAuthor().getUsername())
                            .tags(tags.stream().map(Tag::getName).collect(Collectors.toList()))
                            .chapterCount(chapters.size())
                            .totalReads(totalReads)
                            .publishedAt(a.getPublishedAt())
                            .isFavorited(true)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}
