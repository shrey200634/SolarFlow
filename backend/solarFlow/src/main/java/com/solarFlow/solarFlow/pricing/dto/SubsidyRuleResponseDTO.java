
package com.solarFlow.solarFlow.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SubsidyRuleResponseDTO(
        UUID id,
        BigDecimal subsidyPerKwFirstTwoKw,
        BigDecimal subsidyForThirdKw,
        BigDecimal maximumSubsidy,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        boolean estimated
) {
}
