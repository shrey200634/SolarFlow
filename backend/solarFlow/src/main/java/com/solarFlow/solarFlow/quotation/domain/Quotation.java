package com.solarFlow.solarFlow.quotation.domain;

import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.quotation.domain.QuotationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quotations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal selectedCapacityKw;

    @Column(nullable = false, length = 30)
    private String selectedOptionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant acceptedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = QuotationStatus.ISSUED;
        }
    }
}