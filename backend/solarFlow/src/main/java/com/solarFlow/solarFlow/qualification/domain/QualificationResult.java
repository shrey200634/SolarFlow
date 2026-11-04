package com.solarFlow.solarFlow.qualification.domain;

import com.solarFlow.solarFlow.lead.domain.Lead;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "qualification_results", indexes = {
        @Index(name = "idx_qualification_lead_created", columnList = "lead_id, created_at")
})
public class QualificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false, updatable = false)
    private Lead lead;

    @Column(nullable = false, updatable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_action", nullable = false, length = 30, updatable = false)
    private NextAction nextAction;

    @Column(name = "rule_version", nullable = false, length = 20, updatable = false)
    private String ruleVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons_json", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String reasonsJson;

    @Column(name = "document_id", updatable = false)
    private UUID documentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}