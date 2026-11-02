package com.solarFlow.solarFlow.lead;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponseDTO(
        UUID id,
        Ownership ownership,
        RoofType roofType,
        BigDecimal roofAreaSqft,
        BigDecimal declaredMonthlyBill,
        String pincode,
        String phone,
        LeadStatus status,
        boolean duplicate,   // beacuse we already know form the fronted that this request we already have
        Instant createdAt
) {
    public static LeadResponseDTO from(Lead l) {
        return new LeadResponseDTO(
                l.getId(), l.getOwnership(), l.getRoofType(), l.getRoofAreaSqft(),
                l.getDeclaredMonthlyBill(), l.getPincode(), l.getPhoneE164(),
                l.getStatus(), l.isDuplicateFlag(), l.getCreatedAt());
    }
}