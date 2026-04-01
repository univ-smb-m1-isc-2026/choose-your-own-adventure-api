package com.cyoa.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStateResponse {
    private UUID saveGameId;
    private UUID adventureId;
    private String adventureTitle;
    private Boolean allowBacktrack;
    private ChapterResponse currentChapter;
    private List<ChoiceResponse> availableChoices;
    private Integer health;
    private Integer maxHealth;
    private Map<String, String> stats;
    private Map<String, String> flags;
    private List<InventoryItemResponse> inventory;
    private List<DecisionHistoryResponse> history;
    private Boolean completed;
    private String endingType;
}
