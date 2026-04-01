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
public class AdventureStatsResponse {
    private UUID adventureId;
    private Integer totalReads;
    private Integer totalCompletions;
    private Integer abandonmentCount;
    private Float avgCompletionTime;
    private Long favoriteCount;
}
