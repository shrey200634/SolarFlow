package com.solarFlow.solarFlow.project.domain;

import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.quotation.domain.Quotation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "projects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_quotation", columnNames = "quotation_id"),
        @UniqueConstraint(name = "uk_project_status_token", columnNames = "status_token")
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false, updatable = false)
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false, updatable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    @Column(name = "advance_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal advanceAmount;

    @Column(name = "status_token", nullable = false, length = 64, updatable = false)
    private String statusToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (statusLinkActive == null) statusLinkActive = true;

    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
    @Builder.Default
    @Column(name = "status_link_active")
    private Boolean statusLinkActive = true;


}