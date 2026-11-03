package com.solarFlow.solarFlow.bill.event;

import java.time.Instant;
import java.util.UUID;

public record BillConfirmedEvent(

        UUID leadId,

        UUID documentId,

        UUID confirmedBy,

        Instant confirmedAt

) {
}