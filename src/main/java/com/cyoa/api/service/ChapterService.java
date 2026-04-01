package com.cyoa.api.service;

import com.cyoa.api.dto.request.ChapterRequest;
import com.cyoa.api.dto.request.ChoiceRequest;
import com.cyoa.api.dto.response.ChapterResponse;
import com.cyoa.api.dto.response.ChoiceResponse;
import com.cyoa.api.entity.Adventure;
import com.cyoa.api.entity.Chapter;
import com.cyoa.api.entity.Choice;
import com.cyoa.api.entity.enums.EndingType;
import com.cyoa.api.repository.AdventureRepository;
import com.cyoa.api.repository.ChapterRepository;
import com.cyoa.api.repository.ChoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final AdventureRepository adventureRepository;
    private final ChoiceRepository choiceRepository;

    public ChapterService(ChapterRepository chapterRepository,
                          AdventureRepository adventureRepository,
                          ChoiceRepository choiceRepository) {
        this.chapterRepository = chapterRepository;
        this.adventureRepository = adventureRepository;
        this.choiceRepository = choiceRepository;
    }

    public List<ChapterResponse> getByAdventure(UUID adventureId) {
        List<Chapter> chapters = chapterRepository.findByAdventureId(adventureId);
        return chapters.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ChapterResponse getById(UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        return toResponse(chapter);
    }

    @Transactional
    public ChapterResponse create(UUID adventureId, ChapterRequest request, UUID authorId) {
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("Adventure not found"));
        if (!adventure.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Not the author");
        }

        Chapter chapter = Chapter.builder()
                .adventure(adventure)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .isStart(request.getIsStart() != null ? request.getIsStart() : false)
                .isEnding(request.getIsEnding() != null ? request.getIsEnding() : false)
                .positionX(request.getPositionX())
                .positionY(request.getPositionY())
                .build();

        if (request.getEndingType() != null) {
            try { chapter.setEndingType(EndingType.valueOf(request.getEndingType())); } catch (Exception ignored) {}
        }

        chapter = chapterRepository.save(chapter);

        if (request.getChoices() != null) {
            for (ChoiceRequest cr : request.getChoices()) {
                createChoice(chapter, cr);
            }
        }

        return toResponse(chapterRepository.findById(chapter.getId()).orElseThrow());
    }

    @Transactional
    public ChapterResponse update(UUID chapterId, ChapterRequest request, UUID authorId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        if (!chapter.getAdventure().getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Not the author");
        }

        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setImageUrl(request.getImageUrl());
        if (request.getIsStart() != null) chapter.setIsStart(request.getIsStart());
        if (request.getIsEnding() != null) chapter.setIsEnding(request.getIsEnding());
        if (request.getPositionX() != null) chapter.setPositionX(request.getPositionX());
        if (request.getPositionY() != null) chapter.setPositionY(request.getPositionY());
        if (request.getEndingType() != null) {
            try { chapter.setEndingType(EndingType.valueOf(request.getEndingType())); } catch (Exception ignored) {}
        }

        chapter = chapterRepository.save(chapter);
        return toResponse(chapter);
    }

    @Transactional
    public void delete(UUID chapterId, UUID authorId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        if (!chapter.getAdventure().getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Not the author");
        }
        chapterRepository.delete(chapter);
    }

    private void createChoice(Chapter from, ChoiceRequest cr) {
        Chapter to = chapterRepository.findById(cr.getToChapterId())
                .orElseThrow(() -> new RuntimeException("Target chapter not found"));
        Choice choice = Choice.builder()
                .fromChapter(from)
                .toChapter(to)
                .label(cr.getLabel())
                .displayOrder(cr.getDisplayOrder())
                .requiresConfirmation(cr.getRequiresConfirmation() != null ? cr.getRequiresConfirmation() : false)
                .build();
        choiceRepository.save(choice);
    }

    private ChapterResponse toResponse(Chapter c) {
        List<ChoiceResponse> choices = c.getChoices().stream()
                .map(ch -> ChoiceResponse.builder()
                        .id(ch.getId())
                        .label(ch.getLabel())
                        .toChapterId(ch.getToChapter().getId())
                        .toChapterTitle(ch.getToChapter().getTitle())
                        .displayOrder(ch.getDisplayOrder())
                        .requiresConfirmation(ch.getRequiresConfirmation())
                        .isAvailable(true)
                        .build())
                .collect(Collectors.toList());

        return ChapterResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .imageUrl(c.getImageUrl())
                .isStart(c.getIsStart())
                .isEnding(c.getIsEnding())
                .endingType(c.getEndingType() != null ? c.getEndingType().name() : null)
                .positionX(c.getPositionX())
                .positionY(c.getPositionY())
                .choices(choices)
                .build();
    }
}
