package com.solarFlow.solarFlow.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PricingCalculationDTO(
        BigDecimal systemCapacityKw,
        BigDecimal pricePerKw,
        BigDecimal grossSystemCost,
        BigDecimal subsidyAmount,
        BigDecimal netSystemCost,
        BigDecimal annualGenerationUnits,
        BigDecimal electricityTariffPerUnit,
        BigDecimal estimatedAnnualSavings,
        BigDecimal estimatedPaybackYears,
        boolean subsidyEstimated,
        LocalDate calculationDate,
        UUID pricingRuleId,
        UUID subsidyRuleId
) {}