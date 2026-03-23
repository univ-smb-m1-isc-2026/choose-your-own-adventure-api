package com.cyoa.api.entity;

import com.cyoa.api.entity.enums.EffectType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "effects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Effect {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EffectType type;

    @Column(name = "target_key")
    private String targetKey;

    private String value;
}
