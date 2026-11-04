package com.solarFlow.solarFlow.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PricingRuleResponseDTO(
        UUID id,
        BigDecimal pricePerKw,
        BigDecimal electricityTariffPerUnit,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active
) {
}