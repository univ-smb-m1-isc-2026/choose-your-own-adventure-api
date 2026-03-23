package com.cyoa.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "save_games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveGame {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventure_id", nullable = false)
    private Adventure adventure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_chapter_id")
    private Chapter currentChapter;

    @Column(columnDefinition = "jsonb")
    private String stats;

    @Column(columnDefinition = "jsonb")
    private String flags;

    private Integer health;

    @Column(name = "max_health")
    private Integer maxHealth;

    @Column(name = "last_played")
    private LocalDateTime lastPlayed;

    private Boolean completed;
}
