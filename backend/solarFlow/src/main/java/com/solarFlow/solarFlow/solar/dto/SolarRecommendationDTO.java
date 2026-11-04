package com.solarFlow.solarFlow.solar.dto;

import java.math.BigDecimal;
import java.util.List;

public record SolarRecommendationDTO(
        BigDecimal monthlyUnits,
        BigDecimal roofAreaSqft,
        BigDecimal sanctionedLoadKw,
        BigDecimal usageLimitKw,
        BigDecimal roofLimitKw,
        BigDecimal recommendedCapacityKw,
        String limitingFactor,
        boolean viable,
        String message,
        List<SolarOptionDTO> options
) {

    public record SolarOptionDTO(
            String type,
            BigDecimal capacityKw,
            BigDecimal estimatedYearlyGeneration
    ) {
    }
}