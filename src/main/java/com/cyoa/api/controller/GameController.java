package com.cyoa.api.controller;

import com.cyoa.api.dto.request.MakeChoiceRequest;
import com.cyoa.api.dto.response.GameStateResponse;
import com.cyoa.api.dto.response.SaveGameResponse;
import com.cyoa.api.security.UserPrincipal;
import com.cyoa.api.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start/{adventureId}")
    public ResponseEntity<GameStateResponse> startOrResume(
            @PathVariable UUID adventureId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gameService.startOrResume(adventureId, principal.getUser()));
    }

    @PostMapping("/{saveGameId}/choice")
    public ResponseEntity<GameStateResponse> makeChoice(
            @PathVariable UUID saveGameId,
            @Valid @RequestBody MakeChoiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gameService.makeChoice(saveGameId, request.getChoiceId(), principal.getUser()));
    }

    @GetMapping("/{saveGameId}")
    public ResponseEntity<GameStateResponse> getState(
            @PathVariable UUID saveGameId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gameService.getState(saveGameId, principal.getUser()));
    }

    @PostMapping("/{saveGameId}/backtrack/{step}")
    public ResponseEntity<GameStateResponse> backtrack(
            @PathVariable UUID saveGameId,
            @PathVariable Integer step,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gameService.backtrack(saveGameId, step, principal.getUser()));
    }

    @GetMapping("/saves")
    public ResponseEntity<List<SaveGameResponse>> getMySaves(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gameService.getMySaves(principal.getUser()));
    }
}
