package com.solarFlow.solarFlow.project.controller;

import com.solarFlow.solarFlow.project.domain.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponseDTO(
        UUID id,
        UUID quotationId,
        UUID leadId,
        ProjectStatus status,
        BigDecimal capacityKw,
        BigDecimal netSystemCost,
        BigDecimal advanceAmount,
        String statusLink,
        Instant createdAt,
        Instant updatedAt,
        List<HistoryEntry> history
) {
    public record HistoryEntry(ProjectStatus fromStatus, ProjectStatus toStatus, String note, Instant changedAt) {
    }
}