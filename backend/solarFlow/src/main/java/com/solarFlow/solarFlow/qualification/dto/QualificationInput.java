package com.solarFlow.solarFlow.qualification.dto;

import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.lead.domain.RoofType;

import java.math.BigDecimal;

public record QualificationInput(
        Ownership ownership,
        RoofType roofType,
        BigDecimal roofAreaSqft,
        boolean pincodeServiceable,
        BigDecimal monthlyUnits,
        Double billConfidence

) {
}
