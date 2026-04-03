package com.cyoa.api.service;

import com.cyoa.api.dto.request.ChapterRequest;
import com.cyoa.api.dto.response.ChapterResponse;
import com.cyoa.api.entity.Adventure;
import com.cyoa.api.entity.Chapter;
import com.cyoa.api.entity.Choice;
import com.cyoa.api.entity.SaveGame;
import com.cyoa.api.entity.User;
import com.cyoa.api.entity.enums.AdventureStatus;
import com.cyoa.api.repository.AdventureRepository;
import com.cyoa.api.repository.ChapterRepository;
import com.cyoa.api.repository.ChoiceRepository;
import com.cyoa.api.repository.ConditionRepository;
import com.cyoa.api.repository.DecisionHistoryRepository;
import com.cyoa.api.repository.EffectRepository;
import com.cyoa.api.repository.InventoryItemRepository;
import com.cyoa.api.repository.SaveGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock private ChapterRepository chapterRepository;
    @Mock private AdventureRepository adventureRepository;
    @Mock private ChoiceRepository choiceRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private EffectRepository effectRepository;
    @Mock private SaveGameRepository saveGameRepository;
    @Mock private DecisionHistoryRepository historyRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private ChapterService chapterService;

    private User author;
    private Adventure adventure;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        author = User.builder().id(UUID.randomUUID()).email("a@t.com").username("author").build();
        adventure = Adventure.builder().id(UUID.randomUUID()).author(author).title("Adv").status(AdventureStatus.DRAFT).build();
        chapter = Chapter.builder()
                .id(UUID.randomUUID())
                .adventure(adventure)
                .title("Chapter 1")
                .content("Content here")
                .isStart(true)
                .isEnding(false)
                .choices(new ArrayList<>())
                .build();
    }

    @Test
    void getByAdventure_shouldReturnChapters() {
        when(chapterRepository.findByAdventureId(adventure.getId())).thenReturn(List.of(chapter));

        List<ChapterResponse> result = chapterService.getByAdventure(adventure.getId());

        assertEquals(1, result.size());
        assertEquals("Chapter 1", result.get(0).getTitle());
    }

    @Test
    void getById_shouldReturnChapter() {
        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));

        ChapterResponse result = chapterService.getById(chapter.getId());

        assertNotNull(result);
        assertEquals("Chapter 1", result.getTitle());
        assertTrue(result.getIsStart());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(chapterRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> chapterService.getById(UUID.randomUUID()));
    }

    @Test
    void create_shouldCreateChapter() {
        ChapterRequest request = new ChapterRequest();
        request.setTitle("New Chapter");
        request.setContent("New content");
        request.setIsStart(false);
        request.setIsEnding(false);

        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> {
            Chapter c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setChoices(new ArrayList<>());
            return c;
        });
        when(chapterRepository.findById(any())).thenAnswer(inv -> {
            Chapter c = Chapter.builder()
                    .id(inv.getArgument(0))
                    .adventure(adventure)
                    .title("New Chapter")
                    .content("New content")
                    .isStart(false)
                    .isEnding(false)
                    .choices(new ArrayList<>())
                    .build();
            return Optional.of(c);
        });

        ChapterResponse result = chapterService.create(adventure.getId(), request, author.getId());

        assertNotNull(result);
        assertEquals("New Chapter", result.getTitle());
    }

    @Test
    void create_shouldThrowForNonAuthor() {
        ChapterRequest request = new ChapterRequest();
        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));

        assertThrows(RuntimeException.class, () ->
            chapterService.create(adventure.getId(), request, UUID.randomUUID()));
    }

    @Test
    void update_shouldUpdateChapter() {
        ChapterRequest request = new ChapterRequest();
        request.setTitle("Updated");
        request.setContent("Updated content");

        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(chapterRepository.save(any())).thenReturn(chapter);

        ChapterResponse result = chapterService.update(chapter.getId(), request, author.getId());

        assertEquals("Updated", chapter.getTitle());
    }

    @Test
    void delete_shouldDeleteChapterAfterClearingProgressAndChoices() {
        Choice outgoing = Choice.builder().id(UUID.randomUUID()).fromChapter(chapter).build();
        Choice incoming = Choice.builder().id(UUID.randomUUID()).toChapter(chapter).build();
        SaveGame save = SaveGame.builder().id(UUID.randomUUID()).adventure(adventure).build();

        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(saveGameRepository.findByAdventureId(adventure.getId())).thenReturn(List.of(save));
        when(choiceRepository.findByFromChapterId(chapter.getId())).thenReturn(List.of(outgoing));
        when(choiceRepository.findByToChapterId(chapter.getId())).thenReturn(List.of(incoming));

        chapterService.delete(chapter.getId(), author.getId());

        InOrder inOrder = inOrder(
                historyRepository,
                inventoryItemRepository,
                saveGameRepository,
                conditionRepository,
                effectRepository,
                choiceRepository,
                chapterRepository
        );
        inOrder.verify(historyRepository).deleteBySaveGameId(save.getId());
        inOrder.verify(inventoryItemRepository).deleteBySaveGameId(save.getId());
        inOrder.verify(saveGameRepository).deleteByAdventureId(adventure.getId());
        inOrder.verify(conditionRepository).deleteByChoiceId(outgoing.getId());
        inOrder.verify(effectRepository).deleteByChoiceId(outgoing.getId());
        inOrder.verify(conditionRepository).deleteByChoiceId(incoming.getId());
        inOrder.verify(effectRepository).deleteByChoiceId(incoming.getId());
        inOrder.verify(choiceRepository).deleteByFromChapterId(chapter.getId());
        inOrder.verify(choiceRepository).deleteByToChapterId(chapter.getId());
        inOrder.verify(conditionRepository).deleteByChapterId(chapter.getId());
        inOrder.verify(effectRepository).deleteByChapterId(chapter.getId());
        verify(chapterRepository).delete(chapter);
    }

    @Test
    void delete_shouldThrowForNonAuthor() {
        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));

        assertThrows(RuntimeException.class, () ->
            chapterService.delete(chapter.getId(), UUID.randomUUID()));
    }
}
