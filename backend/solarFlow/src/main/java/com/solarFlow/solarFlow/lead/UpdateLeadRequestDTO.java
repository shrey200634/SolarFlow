package com.solarFlow.solarFlow.lead;

import java.util.UUID;

public record UpdateLeadRequestDTO(
        UUID assignedTo ,
        LeadStatus status
) {
}
