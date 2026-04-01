package com.cyoa.api.service;

import com.cyoa.api.dto.response.GameStateResponse;
import com.cyoa.api.dto.response.SaveGameResponse;
import com.cyoa.api.entity.*;
import com.cyoa.api.entity.enums.AdventureStatus;
import com.cyoa.api.entity.enums.Difficulty;
import com.cyoa.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private SaveGameRepository saveGameRepository;
    @Mock private AdventureRepository adventureRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private ChoiceRepository choiceRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private EffectRepository effectRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private DecisionHistoryRepository historyRepository;
    @Mock private AdventureStatsRepository statsRepository;

    private GameService gameService;

    private User player;
    private Adventure adventure;
    private Chapter startChapter;
    private Chapter endChapter;
    private Choice choice;
    private SaveGame saveGame;

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper since it's not mockable for JSON operations
        gameService = new GameService(
                saveGameRepository, adventureRepository, chapterRepository,
                choiceRepository, conditionRepository, effectRepository,
                inventoryItemRepository, itemRepository, historyRepository,
                statsRepository, new ObjectMapper()
        );

        User author = User.builder().id(UUID.randomUUID()).email("a@t.com").username("author").build();
        player = User.builder().id(UUID.randomUUID()).email("p@t.com").username("player").build();

        adventure = Adventure.builder()
                .id(UUID.randomUUID())
                .author(author)
                .title("Test Adventure")
                .status(AdventureStatus.PUBLISHED)
                .difficulty(Difficulty.MEDIUM)
                .allowBacktrack(true)
                .build();

        startChapter = Chapter.builder()
                .id(UUID.randomUUID())
                .adventure(adventure)
                .title("Start")
                .content("You begin...")
                .isStart(true)
                .isEnding(false)
                .choices(new ArrayList<>())
                .build();

        endChapter = Chapter.builder()
                .id(UUID.randomUUID())
                .adventure(adventure)
                .title("The End")
                .content("You win!")
                .isStart(false)
                .isEnding(true)
                .choices(new ArrayList<>())
                .build();

        choice = Choice.builder()
                .id(UUID.randomUUID())
                .fromChapter(startChapter)
                .toChapter(endChapter)
                .label("Go forward")
                .displayOrder(0)
                .requiresConfirmation(false)
                .build();

        saveGame = SaveGame.builder()
                .id(UUID.randomUUID())
                .user(player)
                .adventure(adventure)
                .currentChapter(startChapter)
                .health(100)
                .maxHealth(100)
                .stats("{}")
                .flags("{}")
                .completed(false)
                .lastPlayed(LocalDateTime.now())
                .build();
    }

    @Test
    void startOrResume_shouldCreateNewSave() {
        when(saveGameRepository.findByUserIdAndAdventureId(player.getId(), adventure.getId()))
                .thenReturn(Optional.empty());
        when(adventureRepository.findById(adventure.getId())).thenReturn(Optional.of(adventure));
        when(chapterRepository.findByAdventureIdAndIsStartTrue(adventure.getId()))
                .thenReturn(Optional.of(startChapter));
        when(saveGameRepository.save(any(SaveGame.class))).thenReturn(saveGame);
        when(historyRepository.save(any())).thenReturn(null);
        when(choiceRepository.findByFromChapterId(any())).thenReturn(List.of(choice));
        when(conditionRepository.findByChoiceId(any())).thenReturn(Collections.emptyList());
        when(inventoryItemRepository.findBySaveGameId(any())).thenReturn(Collections.emptyList());
        when(historyRepository.findBySaveGameIdOrderByStepOrderAsc(any())).thenReturn(Collections.emptyList());
        when(effectRepository.findByChapterId(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        GameStateResponse result = gameService.startOrResume(adventure.getId(), player);

        assertNotNull(result);
        assertEquals("Start", result.getCurrentChapter().getTitle());
        assertEquals(1, result.getAvailableChoices().size());
        assertFalse(result.getCompleted());
    }

    @Test
    void startOrResume_shouldResumeExisting() {
        when(saveGameRepository.findByUserIdAndAdventureId(player.getId(), adventure.getId()))
                .thenReturn(Optional.of(saveGame));
        when(choiceRepository.findByFromChapterId(any())).thenReturn(List.of(choice));
        when(conditionRepository.findByChoiceId(any())).thenReturn(Collections.emptyList());
        when(inventoryItemRepository.findBySaveGameId(any())).thenReturn(Collections.emptyList());
        when(historyRepository.findBySaveGameIdOrderByStepOrderAsc(any())).thenReturn(Collections.emptyList());

        GameStateResponse result = gameService.startOrResume(adventure.getId(), player);

        assertNotNull(result);
        verify(adventureRepository, never()).findById(any());
    }

    @Test
    void makeChoice_shouldMoveToNextChapter() {
        when(saveGameRepository.findById(saveGame.getId())).thenReturn(Optional.of(saveGame));
        when(choiceRepository.findById(choice.getId())).thenReturn(Optional.of(choice));
        when(conditionRepository.findByChoiceId(any())).thenReturn(Collections.emptyList());
        when(effectRepository.findByChoiceId(any())).thenReturn(Collections.emptyList());
        when(effectRepository.findByChapterId(any())).thenReturn(Collections.emptyList());
        when(historyRepository.countBySaveGameId(any())).thenReturn(1L);
        when(historyRepository.save(any())).thenReturn(null);
        when(saveGameRepository.save(any(SaveGame.class))).thenReturn(saveGame);
        when(choiceRepository.findByFromChapterId(any())).thenReturn(Collections.emptyList());
        when(inventoryItemRepository.findBySaveGameId(any())).thenReturn(Collections.emptyList());
        when(historyRepository.findBySaveGameIdOrderByStepOrderAsc(any())).thenReturn(Collections.emptyList());
        when(statsRepository.findById(any())).thenReturn(Optional.empty());

        GameStateResponse result = gameService.makeChoice(saveGame.getId(), choice.getId(), player);

        assertNotNull(result);
        assertTrue(result.getCompleted());
    }

    @Test
    void makeChoice_shouldThrowForWrongUser() {
        User stranger = User.builder().id(UUID.randomUUID()).email("s@t.com").build();
        when(saveGameRepository.findById(saveGame.getId())).thenReturn(Optional.of(saveGame));

        assertThrows(RuntimeException.class, () ->
                gameService.makeChoice(saveGame.getId(), choice.getId(), stranger));
    }

    @Test
    void makeChoice_shouldThrowForCompletedGame() {
        saveGame.setCompleted(true);
        when(saveGameRepository.findById(saveGame.getId())).thenReturn(Optional.of(saveGame));

        assertThrows(RuntimeException.class, () ->
                gameService.makeChoice(saveGame.getId(), choice.getId(), player));
    }

    @Test
    void getMySaves_shouldReturnPlayerSaves() {
        when(saveGameRepository.findByUserId(player.getId())).thenReturn(List.of(saveGame));

        List<SaveGameResponse> result = gameService.getMySaves(player);

        assertEquals(1, result.size());
        assertEquals("Test Adventure", result.get(0).getAdventureTitle());
    }

    @Test
    void getState_shouldReturnState() {
        when(saveGameRepository.findById(saveGame.getId())).thenReturn(Optional.of(saveGame));
        when(choiceRepository.findByFromChapterId(any())).thenReturn(List.of(choice));
        when(conditionRepository.findByChoiceId(any())).thenReturn(Collections.emptyList());
        when(inventoryItemRepository.findBySaveGameId(any())).thenReturn(Collections.emptyList());
        when(historyRepository.findBySaveGameIdOrderByStepOrderAsc(any())).thenReturn(Collections.emptyList());

        GameStateResponse result = gameService.getState(saveGame.getId(), player);

        assertNotNull(result);
        assertEquals(100, result.getHealth());
    }

    @Test
    void getState_shouldThrowForWrongUser() {
        User stranger = User.builder().id(UUID.randomUUID()).email("s@t.com").build();
        when(saveGameRepository.findById(saveGame.getId())).thenReturn(Optional.of(saveGame));

        assertThrows(RuntimeException.class, () ->
                gameService.getState(saveGame.getId(), stranger));
    }
}
