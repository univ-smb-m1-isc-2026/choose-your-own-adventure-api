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
public class AdventureSummaryResponse {
    private UUID id;
    private String title;
    private String summary;
    private Difficulty difficulty;
    private String language;
    private AdventureStatus status;
    private String authorUsername;
    private List<String> tags;
    private Integer chapterCount;
    private Integer totalReads;
    private LocalDateTime publishedAt;
    private Boolean isFavorited;
}
