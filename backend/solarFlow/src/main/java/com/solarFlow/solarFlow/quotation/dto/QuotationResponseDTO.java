package com.solarFlow.solarFlow.quotation.dto;

import com.solarFlow.solarFlow.quotation.domain.QuotationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuotationResponseDTO(

        UUID id,

        UUID leadId,

        BigDecimal selectedCapacityKw,

        String selectedOptionType,

        QuotationStatus status,

        Instant createdAt,

        Instant acceptedAt,

        QuotationSnapshot snapshot

) {
}