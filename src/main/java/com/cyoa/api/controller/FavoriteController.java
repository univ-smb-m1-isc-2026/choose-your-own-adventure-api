package com.cyoa.api.controller;

import com.cyoa.api.dto.response.AdventureSummaryResponse;
import com.cyoa.api.security.UserPrincipal;
import com.cyoa.api.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{adventureId}")
    public ResponseEntity<Map<String, Boolean>> toggle(
            @PathVariable UUID adventureId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean favorited = favoriteService.toggle(principal.getUser().getId(), adventureId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    @GetMapping
    public ResponseEntity<List<AdventureSummaryResponse>> getFavorites(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteService.getFavorites(principal.getUser().getId()));
    }
}
