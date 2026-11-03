package com.solarFlow.solarFlow.bill.controller;

import com.solarFlow.solarFlow.bill.domain.DocumentStatus;

import java.util.Map;
import java.util.UUID;

public record ExtractionResponseDTO(

        UUID documentId,

        DocumentStatus status,

        String provider,

        Map<String, Object> extractedFields,

        Map<String, Object> confidence,

        Map<String, Object> confirmedFields,

        String errorCode

) {
}