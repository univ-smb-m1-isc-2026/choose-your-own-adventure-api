package com.cyoa.api.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ChapterRequest {
    private String title;
    private String content;
    private String imageUrl;
    private Boolean isStart;
    private Boolean isEnding;
    private String endingType;
    private Integer positionX;
    private Integer positionY;
    private List<ChoiceRequest> choices;
}
