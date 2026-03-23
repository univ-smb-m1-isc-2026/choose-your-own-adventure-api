package com.cyoa.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "choices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Choice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_chapter_id", nullable = false)
    private Chapter fromChapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_chapter_id", nullable = false)
    private Chapter toChapter;

    private String label;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "requires_confirmation")
    private Boolean requiresConfirmation;
}
