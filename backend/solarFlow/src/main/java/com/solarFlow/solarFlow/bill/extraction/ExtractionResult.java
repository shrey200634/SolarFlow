package com.solarFlow.solarFlow.bill.extraction;

import java.math.BigDecimal;

public record ExtractionResult(
        Field<String> consumerNumber,
        Field<String> tariff,
        Field<BigDecimal> sanctionedLoadKw,
        Field<BigDecimal> monthlyUnits,
        double overallConfidence,
        String imageQuality,      // GOOD | POOR
        String provider,
        int schemaVersion,
        String rawJson
) {
    public record Field<T>(T value, double confidence) { }
}