package com.solarFlow.solarFlow.lead.controller;

import com.solarFlow.solarFlow.lead.domain.LeadStatus;

import java.util.UUID;

public record UpdateLeadRequestDTO(
        UUID assignedTo ,
        LeadStatus status
) {
}
