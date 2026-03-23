package com.cyoa.api.entity;

import com.cyoa.api.entity.enums.Difficulty;
import com.cyoa.api.entity.enums.AdventureStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "adventures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adventure {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String language;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    private AdventureStatus status;

    @Column(name = "allow_backtrack")
    private Boolean allowBacktrack;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
