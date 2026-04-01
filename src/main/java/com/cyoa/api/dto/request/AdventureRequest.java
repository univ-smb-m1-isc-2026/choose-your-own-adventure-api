package com.cyoa.api.dto.request;

import com.cyoa.api.entity.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AdventureRequest {
    @NotBlank
    private String title;
    private String summary;
    private String language;
    private Difficulty difficulty;
    private Integer estimatedDurationMinutes;
    private Boolean allowBacktrack;
    private List<String> tags;
}
