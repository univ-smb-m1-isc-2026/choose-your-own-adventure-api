package com.cyoa.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "adventure_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdventureStats {
    @Id
    private UUID adventureId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "adventure_id")
    private Adventure adventure;

    @Column(name = "total_reads", nullable = false)
    @Builder.Default
    private Integer totalReads = 0;

    @Column(name = "total_completions", nullable = false)
    @Builder.Default
    private Integer totalCompletions = 0;

    @Column(name = "abandonment_count", nullable = false)
    @Builder.Default
    private Integer abandonmentCount = 0;

    @Column(name = "avg_completion_time")
    @Builder.Default
    private Float avgCompletionTime = 0.0f;
}
