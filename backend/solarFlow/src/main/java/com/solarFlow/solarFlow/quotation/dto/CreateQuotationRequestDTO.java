package com.solarFlow.solarFlow.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateQuotationRequestDTO(

        @NotNull(message = "Selected capacity is required")
        @DecimalMin(value = "0.5", message = "Selected capacity must be at least 0.5 kW")
        BigDecimal selectedCapacityKw

) {
}