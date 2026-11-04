package com.solarFlow.solarFlow.pricing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solar_pricing_rules")
@Getter
@Setter
public class SolarPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerKw;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityTariffPerUnit;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}