package com.solarFlow.solarFlow.pincode.controller;

import com.solarFlow.solarFlow.pincode.domain.ServiceablePincode;

import java.time.Instant;
import java.util.UUID;

public record PincodeResponseDTO(
        UUID id,
        String pincode,
        String areaName,
        boolean active,
        Instant createdAt
) {
    public static PincodeResponseDTO from(ServiceablePincode p) {
        return new PincodeResponseDTO(p.getId(), p.getPincode(), p.getAreaName(), p.isActive(), p.getCreatedAt());
    }
}