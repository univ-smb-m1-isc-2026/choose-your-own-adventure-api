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
public class DecisionHistoryResponse {
    private Integer stepOrder;
    private UUID chapterId;
    private String chapterTitle;
    private UUID choiceId;
    private String choiceLabel;
    private LocalDateTime decidedAt;
}
