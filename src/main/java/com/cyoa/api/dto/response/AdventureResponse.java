package com.cyoa.api.dto.response;

import com.cyoa.api.entity.enums.AdventureStatus;
import com.cyoa.api.entity.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdventureResponse {
    private UUID id;
    private String title;
    private String summary;
    private String language;
    private Difficulty difficulty;
    private Integer estimatedDurationMinutes;
    private AdventureStatus status;
    private Boolean allowBacktrack;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String authorUsername;
    private UUID authorId;
    private List<String> tags;
    private Integer chapterCount;
    private AdventureStatsResponse stats;
    private Boolean isFavorited;
}
