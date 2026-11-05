package com.solarFlow.solarFlow.project.controller;

import com.solarFlow.solarFlow.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectStatusRequestDTO(

        @NotNull(message = "New status is required")
        ProjectStatus status,

        @Size(max = 500)
        String note
) {
}