package com.cyoa.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterResponse {
    private UUID id;
    private String title;
    private String content;
    private String imageUrl;
    private Boolean isStart;
    private Boolean isEnding;
    private String endingType;
    private Integer positionX;
    private Integer positionY;
    private List<ChoiceResponse> choices;
}
