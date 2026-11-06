package com.solarFlow.solarFlow.project.controller;

import jakarta.validation.constraints.NotNull;

public record StatusLinkRequestDTO(
        @NotNull Boolean active
) {
}