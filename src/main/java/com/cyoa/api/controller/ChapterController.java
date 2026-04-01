package com.cyoa.api.controller;

import com.cyoa.api.dto.request.ChapterRequest;
import com.cyoa.api.dto.response.ChapterResponse;
import com.cyoa.api.security.UserPrincipal;
import com.cyoa.api.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping("/chapters/{id}")
    public ResponseEntity<ChapterResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(chapterService.getById(id));
    }

    @PostMapping("/adventures/{adventureId}/chapters")
    public ResponseEntity<ChapterResponse> create(
            @PathVariable UUID adventureId,
            @RequestBody ChapterRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return new ResponseEntity<>(chapterService.create(adventureId, request, principal.getUser().getId()), HttpStatus.CREATED);
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<ChapterResponse> update(
            @PathVariable UUID id,
            @RequestBody ChapterRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chapterService.update(id, request, principal.getUser().getId()));
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        chapterService.delete(id, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
