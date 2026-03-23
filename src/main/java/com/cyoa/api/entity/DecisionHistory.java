package com.cyoa.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "decision_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "save_game_id", nullable = false)
    private SaveGame saveGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
