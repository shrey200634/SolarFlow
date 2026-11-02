package com.solarFlow.solarFlow.lead;

import com.solarFlow.solarFlow.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "leads" , indexes = {

        @Index(name = "idx_lead_phone", columnList = "phone_e164"),
        @Index(name = "idx_lead_status_assigned", columnList = "status, assigned_to")
}
)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Ownership ownership;

    @Enumerated(EnumType.STRING)
    @Column(name = "roof_type", nullable = false, length = 20)
    private RoofType roofType;

    @Column(name = "roof_area_sqft", nullable = false, precision = 8, scale = 1)
    private BigDecimal roofAreaSqft;

    @Column(name = "declared_monthly_bill", nullable = false, precision = 12, scale = 2)
    private BigDecimal declaredMonthlyBill;

    @Column(nullable = false, length = 6, columnDefinition = "char(6)")
    private String pincode;

    @Column(name = "phone_e164", nullable = false, length = 15)
    private String phoneE164;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    // true when another lead with same phone already existed (blueprint: flag, never auto-merge)
    @Column(name = "duplicate_flag", nullable = false)
    @Builder.Default
    private boolean duplicateFlag = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
