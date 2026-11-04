
package com.solarFlow.solarFlow.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubsidyRuleRequestDTO(

        @NotNull(message = "Subsidy per kW for first two kW is required")
        @DecimalMin(value = "0.00", message = "Subsidy cannot be negative")
        @Digits(integer = 10, fraction = 2)
        BigDecimal subsidyPerKwFirstTwoKw,

        @NotNull(message = "Subsidy for third kW is required")
        @DecimalMin(value = "0.00", message = "Subsidy cannot be negative")
        @Digits(integer = 10, fraction = 2)
        BigDecimal subsidyForThirdKw,

        @NotNull(message = "Maximum subsidy is required")
        @DecimalMin(value = "0.01", message = "Maximum subsidy must be positive")
        @Digits(integer = 10, fraction = 2)
        BigDecimal maximumSubsidy,

        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        boolean estimated
) {
}
