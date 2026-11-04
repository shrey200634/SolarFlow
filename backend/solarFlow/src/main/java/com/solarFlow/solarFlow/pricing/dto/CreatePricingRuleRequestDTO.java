package com.solarFlow.solarFlow.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePricingRuleRequestDTO(

        @NotNull(message = "Price per kW is required")
        @DecimalMin(value = "0.01", message = "Price per kW must be greater than zero")
        @Digits(integer = 10, fraction = 2)
        BigDecimal pricePerKw,

        @NotNull(message = "Electricity tariff is required")
        @DecimalMin(value = "0.01", message = "Electricity tariff must be greater than zero")
        @Digits(integer = 8, fraction = 2)
        BigDecimal electricityTariffPerUnit,

        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {
}