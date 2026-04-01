package com.cyoa.api.service;

import com.cyoa.api.dto.request.AdventureRequest;
import com.cyoa.api.dto.response.AdventureResponse;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdventureServiceTest {

    @Mock private AdventureRepository adventureRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private ChoiceRepository choiceRepository;
    @Mock private TagRepository tagRepository;
    @Mock private AdventureStatsRepository statsRepository;
    @Mock private FavoriteRepository favoriteRepository;

    @InjectMocks
    private AdventureService adventureService;

    private User author;
    private Adventure adventure;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(UUID.randomUUID())
                .email("author@test.com")
                .username("author")
                .build();

        adventure = Adventure.builder()
                .id(UUID.randomUUID())
                .author(author)
                .title("Test Adventure")
                .summary("A test adventure")
                .language("fr")
                .difficulty(Difficulty.MEDIUM)
                .status(AdventureStatus.PUBLISHED)
                .allowBacktrack(true)
                .publishedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getCatalogue_shouldReturnPublishedAdventures() {
        when(adventureRepository.findByStatus(AdventureStatus.PUBLISHED))
                .thenReturn(List.of(adventure));
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        List<AdventureSummaryResponse> result = adventureService.getCatalogue(null, null, null, null, "newest", null);

        assertEquals(1, result.size());
        assertEquals("Test Adventure", result.get(0).getTitle());
    }

    @Test
    void getCatalogue_shouldFilterBySearch() {
        when(adventureRepository.findByStatus(AdventureStatus.PUBLISHED))
                .thenReturn(List.of(adventure));
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        List<AdventureSummaryResponse> result = adventureService.getCatalogue("test", null, null, null, "newest", null);
        assertEquals(1, result.size());

        List<AdventureSummaryResponse> empty = adventureService.getCatalogue("nonexistent", null, null, null, "newest", null);
        assertEquals(0, empty.size());
    }

    @Test
    void getCatalogue_shouldFilterByDifficulty() {
        when(adventureRepository.findByStatus(AdventureStatus.PUBLISHED))
                .thenReturn(List.of(adventure));
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        List<AdventureSummaryResponse> result = adventureService.getCatalogue(null, null, Difficulty.MEDIUM, null, "newest", null);
        assertEquals(1, result.size());

        List<AdventureSummaryResponse> empty = adventureService.getCatalogue(null, null, Difficulty.HARD, null, "newest", null);
        assertEquals(0, empty.size());
    }

    @Test
    void getById_shouldReturnAdventure() {
        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        AdventureResponse result = adventureService.getById(adventure.getId(), null);

        assertNotNull(result);
        assertEquals("Test Adventure", result.getTitle());
        assertEquals("author", result.getAuthorUsername());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(adventureRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adventureService.getById(UUID.randomUUID(), null));
    }

    @Test
    void create_shouldCreateAdventure() {
        AdventureRequest request = new AdventureRequest();
        request.setTitle("New Adventure");
        request.setSummary("Summary");
        request.setLanguage("fr");
        request.setDifficulty(Difficulty.EASY);
        request.setTags(List.of("fantasy", "dark"));

        when(adventureRepository.save(any(Adventure.class))).thenAnswer(inv -> {
            Adventure a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(statsRepository.save(any())).thenReturn(null);
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        AdventureResponse result = adventureService.create(request, author);

        assertNotNull(result);
        assertEquals("New Adventure", result.getTitle());
        verify(adventureRepository).save(any(Adventure.class));
    }

    @Test
    void publish_shouldUpdateStatus() {
        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(adventureRepository.save(any(Adventure.class))).thenReturn(adventure);
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        AdventureResponse result = adventureService.publish(adventure.getId(), author.getId());

        assertEquals(AdventureStatus.PUBLISHED, adventure.getStatus());
        assertNotNull(adventure.getPublishedAt());
    }

    @Test
    void publish_shouldThrowForNonAuthor() {
        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));

        UUID otherId = UUID.randomUUID();
        assertThrows(RuntimeException.class, () -> adventureService.publish(adventure.getId(), otherId));
    }

    @Test
    void delete_shouldRemoveAdventure() {
        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());

        adventureService.delete(adventure.getId(), author.getId());

        verify(adventureRepository).delete(adventure);
    }

    @Test
    void getMyAdventures_shouldReturnAuthorAdventures() {
        when(adventureRepository.findByAuthorId(author.getId())).thenReturn(List.of(adventure));
        when(tagRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(chapterRepository.findByAdventureId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        List<AdventureSummaryResponse> result = adventureService.getMyAdventures(author.getId());

        assertEquals(1, result.size());
    }
}
