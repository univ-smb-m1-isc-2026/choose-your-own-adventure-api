package com.cyoa.api.service;

import com.cyoa.api.dto.response.AdventureSummaryResponse;
import com.cyoa.api.entity.*;
import com.cyoa.api.entity.enums.AdventureStatus;
import com.cyoa.api.entity.enums.Difficulty;
import com.cyoa.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private AdventureRepository adventureRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private AdventureStatsRepository statsRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private UUID userId;
    private UUID adventureId;
    private Adventure adventure;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adventureId = UUID.randomUUID();
        User author = User.builder().id(UUID.randomUUID()).email("a@t.com").username("author").build();
        adventure = Adventure.builder()
                .id(adventureId).author(author).title("Test")
                .status(AdventureStatus.PUBLISHED).difficulty(Difficulty.EASY)
                .build();
    }

    @Test
    void toggle_shouldAddFavorite() {
        when(favoriteRepository.existsByIdUserIdAndIdAdventureId(userId, adventureId)).thenReturn(false);
        when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(adventure));
        when(favoriteRepository.save(any())).thenReturn(null);

        boolean result = favoriteService.toggle(userId, adventureId);

        assertTrue(result);
        verify(favoriteRepository).save(any());
    }

    @Test
    void toggle_shouldRemoveFavorite() {
        when(favoriteRepository.existsByIdUserIdAndIdAdventureId(userId, adventureId)).thenReturn(true);

        boolean result = favoriteService.toggle(userId, adventureId);

        assertFalse(result);
        verify(favoriteRepository).deleteByIdUserIdAndIdAdventureId(userId, adventureId);
    }

    @Test
    void getFavorites_shouldReturnList() {
        Favorite fav = Favorite.builder().id(new FavoriteId(userId, adventureId)).build();
        when(favoriteRepository.findByUserId(userId)).thenReturn(List.of(fav));
        when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(adventure));
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        List<AdventureSummaryResponse> result = favoriteService.getFavorites(userId);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsFavorited());
    }

    @Test
    void getFavorites_shouldHandleDeletedAdventure() {
        Favorite fav = Favorite.builder().id(new FavoriteId(userId, adventureId)).build();
        when(favoriteRepository.findByUserId(userId)).thenReturn(List.of(fav));
        when(adventureRepository.findById(adventureId)).thenReturn(Optional.empty());

        List<AdventureSummaryResponse> result = favoriteService.getFavorites(userId);

        assertEquals(0, result.size());
    }
}
