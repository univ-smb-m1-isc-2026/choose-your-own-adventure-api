package com.cyoa.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoiceResponse {
    private UUID id;
    private String label;
    private UUID toChapterId;
    private String toChapterTitle;
    private Integer displayOrder;
    private Boolean requiresConfirmation;
    private Boolean isAvailable;
    private Integer healthDelta;
}
