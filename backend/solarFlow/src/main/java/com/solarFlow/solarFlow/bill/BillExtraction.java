package com.solarFlow.solarFlow.bill;

import com.solarFlow.solarFlow.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bill_extractions",
        uniqueConstraints = @UniqueConstraint(name = "uq_extraction_doc_attempt",
                columnNames = {"document_id", "attempt"}))
public class BillExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private LeadDocument document;

    @Column(nullable = false, length = 30)
    private String provider;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_json", columnDefinition = "jsonb", updatable = false)
    private String extractedJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "confidence_json", columnDefinition = "jsonb")
    private String confidenceJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "confirmed_json", columnDefinition = "jsonb")
    private String confirmedJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}