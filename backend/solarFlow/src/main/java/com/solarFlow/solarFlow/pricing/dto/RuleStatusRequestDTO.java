package com.solarFlow.solarFlow.pricing.dto;

import jakarta.validation.constraints.NotNull;

public record RuleStatusRequestDTO(@NotNull Boolean active) { }