package com.solarFlow.solarFlow.solar.dto;

import java.math.BigDecimal;

public record SolarSizingCalculation(

        BigDecimal usageLimitKw,

        BigDecimal roofLimitKw,

        BigDecimal sanctionedLoadLimitKw,

        BigDecimal recommendedCapacityKw,

        BigDecimal economyCapacityKw,

        BigDecimal maximumCapacityKw,

        String limitingFactor,

        boolean viable

) {
}