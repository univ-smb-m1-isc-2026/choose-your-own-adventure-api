package com.cyoa.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveGameResponse {
    private UUID id;
    private UUID adventureId;
    private String adventureTitle;
    private String currentChapterTitle;
    private Integer health;
    private Integer maxHealth;
    private Boolean completed;
    private LocalDateTime lastPlayed;
}
