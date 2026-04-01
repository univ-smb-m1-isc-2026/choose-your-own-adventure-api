package com.cyoa.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MakeChoiceRequest {
    @NotNull
    private UUID choiceId;
}
