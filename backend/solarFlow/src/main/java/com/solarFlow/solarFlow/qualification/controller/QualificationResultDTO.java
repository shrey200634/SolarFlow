package com.solarFlow.solarFlow.qualification.controller;

import com.solarFlow.solarFlow.qualification.domain.NextAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QualificationResultDTO(
        UUID id,
        UUID leadId,
        int score,
        NextAction nextAction,
        String ruleVersion,
        List<Map<String, Object>> rules,
        List<Map<String, Object>> gates,
        Instant createdAt
) {
}