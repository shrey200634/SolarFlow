package com.solarFlow.solarFlow.lead.controller;

import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.lead.domain.RoofType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateLeadRequestDTO(
        UUID assignedTo,
        LeadStatus status,
        Ownership ownership,
        RoofType roofType,

        @DecimalMin(value = "50.0", message = "roof area must be at least 50 sq ft")
        @DecimalMax(value = "100000.0")
        @Digits(integer = 6, fraction = 1)
        BigDecimal roofAreaSqft
) {
}