package com.cyoa.api.service;

import com.cyoa.api.dto.request.AdventureRequest;
import com.cyoa.api.dto.request.SaveAdventureRequest;
import com.cyoa.api.dto.response.AdventureResponse;
import com.cyoa.api.dto.response.AdventureStatsResponse;
import com.cyoa.api.dto.response.AdventureSummaryResponse;
import com.cyoa.api.dto.response.ChapterResponse;
import com.cyoa.api.dto.response.ChoiceResponse;
import com.cyoa.api.entity.*;
import com.cyoa.api.entity.enums.AdventureStatus;
import com.cyoa.api.entity.enums.Difficulty;
import com.cyoa.api.entity.enums.EndingType;
import com.cyoa.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdventureService {

    private final AdventureRepository adventureRepository;
    private final ChapterRepository chapterRepository;
    private final ChoiceRepository choiceRepository;
    private final TagRepository tagRepository;
    private final AdventureStatsRepository statsRepository;
    private final FavoriteRepository favoriteRepository;

    public AdventureService(AdventureRepository adventureRepository,
                            ChapterRepository chapterRepository,
                            ChoiceRepository choiceRepository,
                            TagRepository tagRepository,
                            AdventureStatsRepository statsRepository,
                            FavoriteRepository favoriteRepository) {
        this.adventureRepository = adventureRepository;
        this.chapterRepository = chapterRepository;
        this.choiceRepository = choiceRepository;
        this.tagRepository = tagRepository;
        this.statsRepository = statsRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public List<AdventureSummaryResponse> getCatalogue(String search, String tag, Difficulty difficulty, String language, String sort, UUID currentUserId) {
        List<Adventure> adventures = new ArrayList<>(adventureRepository.findByStatus(AdventureStatus.PUBLISHED));

        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            adventures = adventures.stream()
                    .filter(a -> a.getTitle().toLowerCase().contains(lower)
                            || (a.getSummary() != null && a.getSummary().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }

        if (difficulty != null) {
            adventures = adventures.stream()
                    .filter(a -> a.getDifficulty() == difficulty)
                    .collect(Collectors.toList());
        }

        if (language != null && !language.isBlank()) {
            adventures = adventures.stream()
                    .filter(a -> language.equalsIgnoreCase(a.getLanguage()))
                    .collect(Collectors.toList());
        }

        if (tag != null && !tag.isBlank()) {
            String tagLower = tag.toLowerCase();
            adventures = adventures.stream()
                    .filter(a -> {
                        List<Tag> tags = tagRepository.findByAdventureId(a.getId());
                        return tags.stream().anyMatch(t -> t.getName().toLowerCase().contains(tagLower));
                    })
                    .collect(Collectors.toList());
        }

        if ("popular".equals(sort)) {
            adventures.sort((a, b) -> {
                int readsA = statsRepository.findById(a.getId()).map(AdventureStats::getTotalReads).orElse(0);
                int readsB = statsRepository.findById(b.getId()).map(AdventureStats::getTotalReads).orElse(0);
                return Integer.compare(readsB, readsA);
            });
        } else {
            adventures.sort(Comparator.comparing(Adventure::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return adventures.stream().map(a -> toSummary(a, currentUserId)).collect(Collectors.toList());
    }

    public AdventureResponse getById(UUID id, UUID currentUserId) {
        Adventure adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adventure not found"));
        return toResponse(adventure, currentUserId);
    }

    public List<AdventureSummaryResponse> getMyAdventures(UUID authorId) {
        List<Adventure> adventures = adventureRepository.findByAuthorId(authorId);
        return adventures.stream().map(a -> toSummary(a, authorId)).collect(Collectors.toList());
    }

    @Transactional
    public AdventureResponse create(AdventureRequest request, User author) {
        Adventure adventure = Adventure.builder()
                .author(author)
                .title(request.getTitle())
                .summary(request.getSummary())
                .language(request.getLanguage())
                .difficulty(request.getDifficulty())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .allowBacktrack(request.getAllowBacktrack() != null ? request.getAllowBacktrack() : true)
                .status(AdventureStatus.DRAFT)
                .build();

        adventure = adventureRepository.save(adventure);
        saveTags(adventure, request.getTags());

        AdventureStats stats = AdventureStats.builder().adventure(adventure).build();
        statsRepository.save(stats);

        return toResponse(adventure, author.getId());
    }

    @Transactional
    public AdventureResponse update(UUID id, AdventureRequest request, UUID authorId) {
        Adventure adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adventure not found"));
        if (!adventure.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Not the author of this adventure");
        }

        adventure.setTitle(request.getTitle());
        adventure.setSummary(request.getSummary());
        adventure.setLanguage(request.getLanguage());
        adventure.setDifficulty(request.getDifficulty());
        adventure.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getAllowBacktrack() != null) {
            adventure.setAllowBacktrack(request.getAllowBacktrack());
        }

        adventure = adventureRepository.save(adventure);
        saveTags(adventure, request.getTags());

        return toResponse(adventure, authorId);
    }

    @Transactional
    public AdventureResponse saveComplete(UUID adventureId, SaveAdventureRequest request, User author) {
        Adventure adventure;
        if (adventureId != null) {
            adventure = adventureRepository.findById(adventureId)
                    .orElseThrow(() -> new RuntimeException("Adventure not found"));
            if (!adventure.getAuthor().getId().equals(author.getId())) {
                throw new RuntimeException("Not the author");
            }
        } else {
            adventure = Adventure.builder()
                    .author(author)
                    .status(AdventureStatus.DRAFT)
                    .allowBacktrack(request.getAllowBacktrack() != null ? request.getAllowBacktrack() : true)
                    .build();
        }

        adventure.setTitle(request.getTitle());
        adventure.setSummary(request.getSummary());
        adventure.setLanguage(request.getLanguage());
        if (request.getDifficulty() != null) {
            try { adventure.setDifficulty(Difficulty.valueOf(request.getDifficulty())); } catch (Exception ignored) {}
        }
        adventure.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getAllowBacktrack() != null) adventure.setAllowBacktrack(request.getAllowBacktrack());
        adventure = adventureRepository.save(adventure);

        if (!statsRepository.existsById(adventure.getId())) {
            statsRepository.save(AdventureStats.builder().adventure(adventure).build());
        }

        saveTags(adventure, request.getTags());

        // Delete existing chapters and choices
        List<Chapter> oldChapters = chapterRepository.findByAdventureId(adventure.getId());
        for (Chapter ch : oldChapters) {
            choiceRepository.deleteAll(ch.getChoices());
        }
        chapterRepository.deleteAll(oldChapters);

        // Create new chapters
        Map<String, Chapter> tempIdToChapter = new HashMap<>();
        if (request.getChapters() != null) {
            for (SaveAdventureRequest.SaveChapterRequest chReq : request.getChapters()) {
                boolean isStart = "start".equals(chReq.getType());
                boolean isEnding = Boolean.TRUE.equals(chReq.getIsEnding()) || "ending".equals(chReq.getType());
                Chapter chapter = Chapter.builder()
                        .adventure(adventure)
                        .title(chReq.getTitle())
                        .content(chReq.getContent())
                        .imageUrl(chReq.getImageUrl())
                        .isStart(isStart)
                        .isEnding(isEnding)
                        .endingType(isEnding ? EndingType.NEUTRAL : null)
                        .positionX(chReq.getPositionX())
                        .positionY(chReq.getPositionY())
                        .build();
                chapter = chapterRepository.save(chapter);
                String key = chReq.getTempId() != null ? chReq.getTempId() : chapter.getId().toString();
                tempIdToChapter.put(key, chapter);
            }
        }

        // Create edges as choices
        if (request.getEdges() != null) {
            int order = 0;
            for (SaveAdventureRequest.SaveEdgeRequest edgeReq : request.getEdges()) {
                Chapter from = tempIdToChapter.get(edgeReq.getSourceId());
                Chapter to = tempIdToChapter.get(edgeReq.getTargetId());
                if (from != null && to != null) {
                    Choice choice = Choice.builder()
                            .fromChapter(from)
                            .toChapter(to)
                            .label(edgeReq.getLabel() != null ? edgeReq.getLabel() : "Continuer")
                            .displayOrder(order++)
                            .requiresConfirmation(false)
                            .build();
                    choiceRepository.save(choice);
                }
            }
        }

        return toResponse(adventure, author.getId());
    }

    @Transactional
    public AdventureResponse publish(UUID id, UUID authorId) {
        Adventure adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adventure not found"));
        if (!adventure.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Not the author");
        }
        adventure.setStatus(AdventureStatus.PUBLISHED);
        adventure.setPublishedAt(LocalDateTime.now());
        adventure = adventureRepository.save(adventure);
        return toResponse(adventure, authorId);
    }

    @Transactional
    public void delete(UUID id, UUID authorId) {
        Adventure adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adventure not found"));
        if (!adventure.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Not the author");
        }
        tagRepository.deleteByAdventureId(id);
        List<Chapter> chapters = chapterRepository.findByAdventureId(id);
        for (Chapter ch : chapters) {
            choiceRepository.deleteAll(ch.getChoices());
        }
        chapterRepository.deleteAll(chapters);
        statsRepository.deleteById(id);
        adventureRepository.delete(adventure);
    }

    public List<ChapterResponse> getChapters(UUID adventureId) {
        List<Chapter> chapters = chapterRepository.findByAdventureId(adventureId);
        return chapters.stream().map(this::toChapterResponse).collect(Collectors.toList());
    }

    private void saveTags(Adventure adventure, List<String> tagNames) {
        tagRepository.deleteByAdventureId(adventure.getId());
        if (tagNames != null) {
            for (String name : tagNames) {
                if (name != null && !name.isBlank()) {
                    Tag tag = Tag.builder().adventure(adventure).name(name.trim()).build();
                    tagRepository.save(tag);
                }
            }
        }
    }

    private AdventureSummaryResponse toSummary(Adventure a, UUID currentUserId) {
        List<Tag> tags = tagRepository.findByAdventureId(a.getId());
        List<Chapter> chapters = chapterRepository.findByAdventureId(a.getId());
        int totalReads = statsRepository.findById(a.getId()).map(AdventureStats::getTotalReads).orElse(0);
        boolean fav = currentUserId != null && favoriteRepository.existsByIdUserIdAndIdAdventureId(currentUserId, a.getId());

        return AdventureSummaryResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .summary(a.getSummary())
                .difficulty(a.getDifficulty())
                .language(a.getLanguage())
                .status(a.getStatus())
                .authorUsername(a.getAuthor().getUsername())
                .tags(tags.stream().map(Tag::getName).collect(Collectors.toList()))
                .chapterCount(chapters.size())
                .totalReads(totalReads)
                .publishedAt(a.getPublishedAt())
                .isFavorited(fav)
                .build();
    }

    private AdventureResponse toResponse(Adventure a, UUID currentUserId) {
        List<Tag> tags = tagRepository.findByAdventureId(a.getId());
        List<Chapter> chapters = chapterRepository.findByAdventureId(a.getId());
        AdventureStats stats = statsRepository.findById(a.getId()).orElse(null);
        boolean fav = currentUserId != null && favoriteRepository.existsByIdUserIdAndIdAdventureId(currentUserId, a.getId());

        AdventureStatsResponse statsResp = null;
        if (stats != null) {
            statsResp = AdventureStatsResponse.builder()
                    .adventureId(a.getId())
                    .totalReads(stats.getTotalReads())
                    .totalCompletions(stats.getTotalCompletions())
                    .abandonmentCount(stats.getAbandonmentCount())
                    .avgCompletionTime(stats.getAvgCompletionTime())
                    .favoriteCount(favoriteRepository.countByIdAdventureId(a.getId()))
                    .build();
        }

        return AdventureResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .summary(a.getSummary())
                .language(a.getLanguage())
                .difficulty(a.getDifficulty())
                .estimatedDurationMinutes(a.getEstimatedDurationMinutes())
                .status(a.getStatus())
                .allowBacktrack(a.getAllowBacktrack())
                .createdAt(a.getCreatedAt())
                .publishedAt(a.getPublishedAt())
                .authorUsername(a.getAuthor().getUsername())
                .authorId(a.getAuthor().getId())
                .tags(tags.stream().map(Tag::getName).collect(Collectors.toList()))
                .chapterCount(chapters.size())
                .stats(statsResp)
                .isFavorited(fav)
                .build();
    }

    private ChapterResponse toChapterResponse(Chapter c) {
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
