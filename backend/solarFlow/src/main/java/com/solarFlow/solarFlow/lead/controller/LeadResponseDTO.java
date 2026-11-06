package com.solarFlow.solarFlow.lead.controller;

import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.lead.domain.RoofType;
import com.solarFlow.solarFlow.user.domain.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponseDTO(
        UUID id,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        UUID assignedToId,
        String assignedToName,
        Ownership ownership,
        RoofType roofType,
        BigDecimal roofAreaSqft,
        BigDecimal declaredMonthlyBill,
        String pincode,
        String phone,
        LeadStatus status,
        boolean duplicate,
        Integer qualificationScore,   // latest score, null if the lead has not been qualified
        Instant createdAt
) {
    public static LeadResponseDTO from(Lead l, Integer qualificationScore) {
        User owner = l.getOwner();
        User assignee = l.getAssignedTo();
        return new LeadResponseDTO(
                l.getId(),
                owner.getId(),
                owner.getFullName(),
                owner.getEmail(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                l.getOwnership(), l.getRoofType(), l.getRoofAreaSqft(),
                l.getDeclaredMonthlyBill(), l.getPincode(), l.getPhoneE164(),
                l.getStatus(), l.isDuplicateFlag(), qualificationScore, l.getCreatedAt());
    }
}