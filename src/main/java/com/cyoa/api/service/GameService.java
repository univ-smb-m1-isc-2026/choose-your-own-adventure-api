package com.cyoa.api.service;

import com.cyoa.api.dto.response.*;
import com.cyoa.api.entity.*;
import com.cyoa.api.entity.enums.ConditionType;
import com.cyoa.api.entity.enums.EffectType;
import com.cyoa.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final SaveGameRepository saveGameRepository;
    private final AdventureRepository adventureRepository;
    private final ChapterRepository chapterRepository;
    private final ChoiceRepository choiceRepository;
    private final ConditionRepository conditionRepository;
    private final EffectRepository effectRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ItemRepository itemRepository;
    private final DecisionHistoryRepository historyRepository;
    private final AdventureStatsRepository statsRepository;
    private final ObjectMapper objectMapper;

    public GameService(SaveGameRepository saveGameRepository,
                       AdventureRepository adventureRepository,
                       ChapterRepository chapterRepository,
                       ChoiceRepository choiceRepository,
                       ConditionRepository conditionRepository,
                       EffectRepository effectRepository,
                       InventoryItemRepository inventoryItemRepository,
                       ItemRepository itemRepository,
                       DecisionHistoryRepository historyRepository,
                       AdventureStatsRepository statsRepository,
                       ObjectMapper objectMapper) {
        this.saveGameRepository = saveGameRepository;
        this.adventureRepository = adventureRepository;
        this.chapterRepository = chapterRepository;
        this.choiceRepository = choiceRepository;
        this.conditionRepository = conditionRepository;
        this.effectRepository = effectRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.statsRepository = statsRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GameStateResponse startOrResume(UUID adventureId, User user) {
        List<SaveGame> existingSaves = saveGameRepository.findByUserIdAndAdventureIdOrderByLastPlayedDesc(user.getId(), adventureId);
        
        Optional<SaveGame> existing = existingSaves.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getCompleted()))
                .findFirst();

        if (existing.isPresent()) {
            return buildGameState(existing.get());
        }

        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("Adventure not found"));

        Chapter startChapter = chapterRepository.findByAdventureIdAndIsStartTrue(adventureId)
                .orElseThrow(() -> new RuntimeException("No start chapter found"));

        // Increment read count
        statsRepository.findById(adventureId).ifPresent(s -> {
            s.setTotalReads(s.getTotalReads() + 1);
            statsRepository.save(s);
        });

        SaveGame save = SaveGame.builder()
                .user(user)
                .adventure(adventure)
                .currentChapter(startChapter)
                .playerName(user.getUsername() != null ? user.getUsername() : user.getEmail())
                .health(100)
                .maxHealth(100)
                .stats("{}")
                .flags("{}")
                .lastPlayed(LocalDateTime.now())
                .completed(false)
                .build();
        save = saveGameRepository.save(save);

        // Record first chapter in history
        DecisionHistory entry = DecisionHistory.builder()
                .saveGame(save)
                .chapter(startChapter)
                .stepOrder(0)
                .decidedAt(LocalDateTime.now())
                .build();
        historyRepository.save(entry);

        // Apply chapter effects
        applyChapterEffects(save, startChapter);

        return buildGameState(save);
    }

    @Transactional
    public GameStateResponse restart(UUID adventureId, User user) {
        List<SaveGame> existingSaves = saveGameRepository.findByUserIdAndAdventureIdOrderByLastPlayedDesc(user.getId(), adventureId);
        for (SaveGame save : existingSaves) {
            // Delete history and inventory
            historyRepository.deleteBySaveGameIdAndStepOrderGreaterThan(save.getId(), -1);
            inventoryItemRepository.findBySaveGameId(save.getId())
                    .forEach(inventoryItemRepository::delete);
            saveGameRepository.delete(save);
        }
        return startOrResume(adventureId, user);
    }

    @Transactional
    public GameStateResponse updateCombatResult(UUID saveGameId, int newHealth, User user) {
        SaveGame save = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> new RuntimeException("Save game not found"));
        if (!save.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your save game");
        }
        save.setHealth(Math.max(0, Math.min(newHealth, save.getMaxHealth())));
        save.setLastPlayed(LocalDateTime.now());

        // If player died, mark as completed
        if (save.getHealth() <= 0) {
            save.setCompleted(true);
        } else {
            // Victory transition: move to the next chapter via the first choice
            List<Choice> choices = choiceRepository.findByFromChapterId(save.getCurrentChapter().getId());
            if (!choices.isEmpty()) {
                Choice victoryChoice = choices.get(0);
                Chapter nextChapter = victoryChoice.getToChapter();
                save.setCurrentChapter(nextChapter);

                // Record history
                long stepCount = historyRepository.countBySaveGameId(save.getId());
                DecisionHistory entry = DecisionHistory.builder()
                        .saveGame(save)
                        .chapter(nextChapter)
                        .choice(victoryChoice)
                        .stepOrder((int) stepCount)
                        .decidedAt(LocalDateTime.now())
                        .build();
                historyRepository.save(entry);

                // Apply chapter effects
                applyChapterEffects(save, nextChapter);

                // Check if ending
                if (Boolean.TRUE.equals(nextChapter.getIsEnding())) {
                    save.setCompleted(true);
                    statsRepository.findById(save.getAdventure().getId()).ifPresent(s -> {
                        s.setTotalCompletions(s.getTotalCompletions() + 1);
                        statsRepository.save(s);
                    });
                }
            }
        }

        save = saveGameRepository.save(save);
        return buildGameState(save);
    }

    @Transactional
    public GameStateResponse makeChoice(UUID saveGameId, UUID choiceId, User user) {
        SaveGame save = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> new RuntimeException("Save game not found"));

        if (!save.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your save game");
        }

        if (Boolean.TRUE.equals(save.getCompleted())) {
            throw new RuntimeException("Adventure already completed");
        }

        Choice choice = choiceRepository.findById(choiceId)
                .orElseThrow(() -> new RuntimeException("Choice not found"));

        if (!choice.getFromChapter().getId().equals(save.getCurrentChapter().getId())) {
            throw new RuntimeException("Invalid choice for current chapter");
        }

        // Check conditions
        if (!evaluateConditions(save, choice)) {
            throw new RuntimeException("Conditions not met for this choice");
        }

        // Apply choice effects
        applyChoiceEffects(save, choice);

        // Move to next chapter
        Chapter nextChapter = choice.getToChapter();
        save.setCurrentChapter(nextChapter);
        save.setLastPlayed(LocalDateTime.now());

        // Record decision
        long stepCount = historyRepository.countBySaveGameId(save.getId());
        DecisionHistory entry = DecisionHistory.builder()
                .saveGame(save)
                .chapter(nextChapter)
                .choice(choice)
                .stepOrder((int) stepCount)
                .decidedAt(LocalDateTime.now())
                .build();
        historyRepository.save(entry);

        // Apply chapter effects
        applyChapterEffects(save, nextChapter);

        // Check if ending
        if (Boolean.TRUE.equals(nextChapter.getIsEnding())) {
            save.setCompleted(true);
            statsRepository.findById(save.getAdventure().getId()).ifPresent(s -> {
                s.setTotalCompletions(s.getTotalCompletions() + 1);
                statsRepository.save(s);
            });
        }

        save = saveGameRepository.save(save);
        return buildGameState(save);
    }

    @Transactional
    public GameStateResponse backtrack(UUID saveGameId, Integer toStep, User user) {
        SaveGame save = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> new RuntimeException("Save game not found"));

        if (!save.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your save game");
        }

        Adventure adventure = save.getAdventure();
        if (!Boolean.TRUE.equals(adventure.getAllowBacktrack())) {
            throw new RuntimeException("Backtracking not allowed in this adventure");
        }

        List<DecisionHistory> history = historyRepository.findBySaveGameIdOrderByStepOrderAsc(save.getId());
        DecisionHistory targetStep = history.stream()
                .filter(h -> h.getStepOrder().equals(toStep))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Step not found in history"));

        // Remove future history
        historyRepository.deleteBySaveGameIdAndStepOrderGreaterThan(save.getId(), toStep);

        // Reset to that chapter
        save.setCurrentChapter(targetStep.getChapter());
        save.setCompleted(false);
        save.setLastPlayed(LocalDateTime.now());
        save = saveGameRepository.save(save);

        return buildGameState(save);
    }

    public List<SaveGameResponse> getMySaves(User user) {
        List<SaveGame> saves = saveGameRepository.findByUserId(user.getId());
        return saves.stream().map(s -> SaveGameResponse.builder()
                .id(s.getId())
                .adventureId(s.getAdventure().getId())
                .adventureTitle(s.getAdventure().getTitle())
                .currentChapterTitle(s.getCurrentChapter() != null ? s.getCurrentChapter().getTitle() : null)
                .health(s.getHealth())
                .maxHealth(s.getMaxHealth())
                .completed(s.getCompleted())
                .lastPlayed(s.getLastPlayed())
                .build())
                .collect(Collectors.toList());
    }

    public GameStateResponse getState(UUID saveGameId, User user) {
        SaveGame save = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> new RuntimeException("Save game not found"));
        if (!save.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your save game");
        }
        return buildGameState(save);
    }

    private boolean evaluateConditions(SaveGame save, Choice choice) {
        List<Condition> conditions = conditionRepository.findByChoiceId(choice.getId());
        if (conditions.isEmpty()) return true;

        Map<String, String> stats = parseJson(save.getStats());
        Map<String, String> flags = parseJson(save.getFlags());
        List<InventoryItem> inventory = inventoryItemRepository.findBySaveGameId(save.getId());

        for (Condition cond : conditions) {
            boolean met = switch (cond.getType()) {
                case HAS_ITEM -> inventory.stream()
                        .anyMatch(i -> i.getItem().getName().equals(cond.getTargetKey()) && i.getQuantity() > 0);
                case STAT_CHECK -> {
                    String val = stats.getOrDefault(cond.getTargetKey(), "0");
                    try {
                        int v = Integer.parseInt(val);
                        int target = Integer.parseInt(cond.getTargetValue());
                        yield switch (cond.getOperator()) {
                            case ">=" -> v >= target;
                            case "<=" -> v <= target;
                            case ">" -> v > target;
                            case "<" -> v < target;
                            case "==" -> v == target;
                            default -> v >= target;
                        };
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case FLAG -> flags.containsKey(cond.getTargetKey())
                        && flags.get(cond.getTargetKey()).equals(cond.getTargetValue());
                case VISITED_CHAPTER -> {
                    List<DecisionHistory> h = historyRepository.findBySaveGameIdOrderByStepOrderAsc(save.getId());
                    yield h.stream().anyMatch(d -> d.getChapter().getId().toString().equals(cond.getTargetValue()));
                }
            };
            if (!met) return false;
        }
        return true;
    }

    private void applyChoiceEffects(SaveGame save, Choice choice) {
        List<Effect> effects = effectRepository.findByChoiceId(choice.getId());
        applyEffects(save, effects);
    }

    private void applyChapterEffects(SaveGame save, Chapter chapter) {
        List<Effect> effects = effectRepository.findByChapterId(chapter.getId());
        applyEffects(save, effects);
    }

    private void applyEffects(SaveGame save, List<Effect> effects) {
        Map<String, String> stats = parseJson(save.getStats());
        Map<String, String> flags = parseJson(save.getFlags());

        for (Effect effect : effects) {
            switch (effect.getType()) {
                case MODIFY_STAT -> {
                    int current = 0;
                    try { current = Integer.parseInt(stats.getOrDefault(effect.getTargetKey(), "0")); }
                    catch (Exception ignored) {}
                    int delta = 0;
                    try { delta = Integer.parseInt(effect.getValue()); } catch (Exception ignored) {}
                    stats.put(effect.getTargetKey(), String.valueOf(current + delta));

                    if ("health".equals(effect.getTargetKey())) {
                        int newHealth = save.getHealth() + delta;
                        save.setHealth(Math.max(0, Math.min(newHealth, save.getMaxHealth())));
                    }
                }
                case SET_FLAG -> flags.put(effect.getTargetKey(), effect.getValue());
                case ADD_ITEM -> {
                    Item item = itemRepository.findByAdventureIdAndName(
                            save.getAdventure().getId(), effect.getTargetKey()).orElse(null);
                    if (item != null) {
                        Optional<InventoryItem> inv = inventoryItemRepository.findBySaveGameIdAndItemId(save.getId(), item.getId());
                        if (inv.isPresent()) {
                            InventoryItem ii = inv.get();
                            int qty = 1;
                            try { qty = Integer.parseInt(effect.getValue()); } catch (Exception ignored) {}
                            ii.setQuantity(ii.getQuantity() + qty);
                            inventoryItemRepository.save(ii);
                        } else {
                            int qty = 1;
                            try { qty = Integer.parseInt(effect.getValue()); } catch (Exception ignored) {}
                            InventoryItem ii = InventoryItem.builder()
                                    .saveGame(save).item(item).quantity(qty).build();
                            inventoryItemRepository.save(ii);
                        }
                    }
                }
                case REMOVE_ITEM -> {
                    Item item = itemRepository.findByAdventureIdAndName(
                            save.getAdventure().getId(), effect.getTargetKey()).orElse(null);
                    if (item != null) {
                        inventoryItemRepository.findBySaveGameIdAndItemId(save.getId(), item.getId())
                                .ifPresent(ii -> {
                                    int qty = 1;
                                    try { qty = Integer.parseInt(effect.getValue()); } catch (Exception ignored) {}
                                    ii.setQuantity(Math.max(0, ii.getQuantity() - qty));
                                    inventoryItemRepository.save(ii);
                                });
                    }
                }
                case COMBAT -> {
                    // Simple combat: reduce health based on effect value
                    int damage = 10;
                    try { damage = Integer.parseInt(effect.getValue()); } catch (Exception ignored) {}
                    save.setHealth(Math.max(0, save.getHealth() - damage));
                }
            }
        }

        try {
            save.setStats(objectMapper.writeValueAsString(stats));
            save.setFlags(objectMapper.writeValueAsString(flags));
        } catch (Exception ignored) {}
        saveGameRepository.save(save);
    }

    private GameStateResponse buildGameState(SaveGame save) {
        Chapter current = save.getCurrentChapter();
        List<Choice> choices = choiceRepository.findByFromChapterId(current.getId());

        List<ChoiceResponse> availableChoices = choices.stream()
                .sorted(Comparator.comparing(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 0))
                .map(c -> {
                    // Compute healthDelta from effects
                    List<Effect> effects = effectRepository.findByChoiceId(c.getId());
                    Integer healthDelta = effects.stream()
                            .filter(e -> e.getType() == com.cyoa.api.entity.enums.EffectType.MODIFY_STAT && "health".equals(e.getTargetKey()))
                            .map(e -> { try { return Integer.parseInt(e.getValue()); } catch (Exception ex) { return 0; } })
                            .reduce(0, Integer::sum);
                    return ChoiceResponse.builder()
                        .id(c.getId())
                        .label(c.getLabel())
                        .toChapterId(c.getToChapter().getId())
                        .displayOrder(c.getDisplayOrder())
                        .requiresConfirmation(c.getRequiresConfirmation())
                        .isAvailable(evaluateConditions(save, c))
                        .healthDelta(healthDelta != 0 ? healthDelta : null)
                        .build();
                })
                .collect(Collectors.toList());

        ChapterResponse chapterResp = ChapterResponse.builder()
                .id(current.getId())
                .title(current.getTitle())
                .content(current.getContent())
                .imageUrl(current.getImageUrl())
                .isStart(current.getIsStart())
                .isEnding(current.getIsEnding())
                .endingType(current.getEndingType() != null ? current.getEndingType().name() : null)
                .isCombat(current.getIsCombat())
                .combatEnemyName(current.getCombatEnemyName())
                .combatEnemyHealth(current.getCombatEnemyHealth())
                .build();

        List<InventoryItem> invItems = inventoryItemRepository.findBySaveGameId(save.getId());
        List<InventoryItemResponse> inventory = invItems.stream()
                .filter(i -> i.getQuantity() > 0)
                .map(i -> InventoryItemResponse.builder()
                        .itemId(i.getItem().getId())
                        .name(i.getItem().getName())
                        .description(i.getItem().getDescription())
                        .imageUrl(i.getItem().getImageUrl())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        List<DecisionHistory> historyEntries = historyRepository.findBySaveGameIdOrderByStepOrderAsc(save.getId());
        List<DecisionHistoryResponse> history = historyEntries.stream()
                .map(h -> DecisionHistoryResponse.builder()
                        .stepOrder(h.getStepOrder())
                        .chapterId(h.getChapter().getId())
                        .chapterTitle(h.getChapter().getTitle())
                        .choiceId(h.getChoice() != null ? h.getChoice().getId() : null)
                        .choiceLabel(h.getChoice() != null ? h.getChoice().getLabel() : null)
                        .decidedAt(h.getDecidedAt())
                        .build())
                .collect(Collectors.toList());

        Map<String, String> stats = parseJson(save.getStats());
        Map<String, String> flags = parseJson(save.getFlags());

        return GameStateResponse.builder()
                .saveGameId(save.getId())
                .adventureId(save.getAdventure().getId())
                .adventureTitle(save.getAdventure().getTitle())
                .allowBacktrack(save.getAdventure().getAllowBacktrack())
                .currentChapter(chapterResp)
                .availableChoices(availableChoices)
                .health(save.getHealth())
                .maxHealth(save.getMaxHealth())
                .stats(stats)
                .flags(flags)
                .inventory(inventory)
                .history(history)
                .completed(save.getCompleted())
                .endingType(current.getEndingType() != null ? current.getEndingType().name() : null)
                .build();
    }

    private Map<String, String> parseJson(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
