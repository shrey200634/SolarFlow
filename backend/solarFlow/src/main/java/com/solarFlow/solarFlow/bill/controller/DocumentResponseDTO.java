package com.solarFlow.solarFlow.bill.controller;

import com.solarFlow.solarFlow.bill.domain.DocumentStatus;
import com.solarFlow.solarFlow.bill.domain.LeadDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponseDTO(
        UUID id,
        UUID leadId,
        String contentType,
        long sizeBytes,
        DocumentStatus status,
        Instant createdAt
) {
    public static DocumentResponseDTO from(LeadDocument d) {
        return new DocumentResponseDTO(d.getId(), d.getLead().getId(), d.getContentType(),
                d.getSizeBytes(), d.getStatus(), d.getCreatedAt());
    }
}