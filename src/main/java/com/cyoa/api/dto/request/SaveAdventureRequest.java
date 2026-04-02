package com.cyoa.api.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SaveAdventureRequest {
    private String title;
    private String summary;
    private String language;
    private String difficulty;
    private Integer estimatedDurationMinutes;
    private Boolean allowBacktrack;
    private List<String> tags;
    private List<SaveChapterRequest> chapters;
    private List<SaveEdgeRequest> edges;

    @Data
    public static class SaveChapterRequest {
        private String tempId;
        private UUID existingId;
        private String title;
        private String content;
        private String imageUrl;
        private String type;
        private Boolean isEnding;
        private Integer positionX;
        private Integer positionY;
        private String combatEnemyName;
        private Integer combatEnemyHealth;
    }

    @Data
    public static class SaveEdgeRequest {
        private String sourceId;
        private String targetId;
        private String label;
        private Integer healthDelta;
        private Boolean requiresConfirmation;
    }
}
