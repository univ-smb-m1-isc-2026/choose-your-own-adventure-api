package com.cyoa.api.controller;

import com.cyoa.api.dto.request.AdventureRequest;
import com.cyoa.api.dto.request.SaveAdventureRequest;
import com.cyoa.api.dto.response.AdventureResponse;
import com.cyoa.api.dto.response.AdventureSummaryResponse;
import com.cyoa.api.dto.response.ChapterResponse;
import com.cyoa.api.entity.User;
import com.cyoa.api.entity.enums.Difficulty;
import com.cyoa.api.security.UserPrincipal;
import com.cyoa.api.service.AdventureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/adventures")
public class AdventureController {

    private final AdventureService adventureService;

    public AdventureController(AdventureService adventureService) {
        this.adventureService = adventureService;
    }

    @GetMapping
    public ResponseEntity<List<AdventureSummaryResponse>> getCatalogue(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getUser().getId() : null;
        return ResponseEntity.ok(adventureService.getCatalogue(search, tag, difficulty, language, sort, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdventureResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getUser().getId() : null;
        return ResponseEntity.ok(adventureService.getById(id, userId));
    }

    @GetMapping("/{id}/chapters")
    public ResponseEntity<List<ChapterResponse>> getChapters(@PathVariable UUID id) {
        return ResponseEntity.ok(adventureService.getChapters(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<AdventureSummaryResponse>> getMyAdventures(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adventureService.getMyAdventures(principal.getUser().getId()));
    }

    @PostMapping
    public ResponseEntity<AdventureResponse> create(
            @Valid @RequestBody AdventureRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return new ResponseEntity<>(adventureService.create(request, principal.getUser()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdventureResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdventureRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adventureService.update(id, request, principal.getUser().getId()));
    }

    @PostMapping("/save")
    public ResponseEntity<AdventureResponse> saveComplete(
            @RequestParam(required = false) UUID adventureId,
            @RequestBody SaveAdventureRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adventureService.saveComplete(adventureId, request, principal.getUser()));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<AdventureResponse> publish(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adventureService.publish(id, principal.getUser().getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        adventureService.delete(id, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
