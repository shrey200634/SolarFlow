package com.solarFlow.solarFlow.bill;

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