package com.cyoa.api.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ChoiceRequest {
    private String label;
    private UUID toChapterId;
    private Integer displayOrder;
    private Boolean requiresConfirmation;
}
