package com.solarFlow.solarFlow.project.controller;

import com.solarFlow.solarFlow.project.domain.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What the unauthenticated status link shows. Deliberately minimal: no names, address,
 * consumer number, bill, prices, notes or internal ids.
 */
public record PublicProjectStatusDTO(
        ProjectStatus currentStatus,
        BigDecimal capacityKw,
        Instant startedAt,
        Instant lastUpdatedAt,
        List<Stage> timeline
) {
    public record Stage(ProjectStatus status, Instant at) {
    }
}