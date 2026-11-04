package com.solarFlow.solarFlow.qualification.dto;

import com.solarFlow.solarFlow.lead.domain.RoofType;

import java.math.BigDecimal;
import java.util.Set;

public record QualificationRules(
        String ruleVersion,
        int surveyThreshold,
        BigDecimal minMonthlyUnits,
        BigDecimal roofAreaThresholdSqft,
        Set<RoofType> suitableRoofTypes,
        double minBillConfidence,
        int weightOwner,
        int weightRoofType,
        int weightRoofArea,
        int weightUnits,
        int weightPincode,
        int weightBillConfidence
) {
}
