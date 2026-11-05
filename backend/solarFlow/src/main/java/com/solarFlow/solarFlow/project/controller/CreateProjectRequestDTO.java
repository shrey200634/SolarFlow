package com.solarFlow.solarFlow.project.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProjectRequestDTO(

        @NotNull(message = "Quotation id is required")
        UUID quotationId,

        @NotNull(message = "Advance amount is required")
        @DecimalMin(value = "0.01", message = "Advance amount must be greater than zero")
        @Digits(integer = 12, fraction = 2)
        BigDecimal advanceAmount
) {
}