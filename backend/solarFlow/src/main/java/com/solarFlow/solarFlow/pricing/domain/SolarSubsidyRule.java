package com.solarFlow.solarFlow.pricing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solar_subsidy_rules")
@Getter
@Setter
public class SolarSubsidyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subsidyPerKwFirstTwoKw;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subsidyForThirdKw;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal maximumSubsidy;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean estimated = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}