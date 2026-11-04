package com.solarFlow.solarFlow.qualification.service;

import com.solarFlow.solarFlow.lead.domain.RoofType;
import com.solarFlow.solarFlow.qualification.dto.QualificationRules;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "solarflow.qualification")
@Getter
@Setter
public class QualificationProperties {

    private String ruleVersion = "v1";
    private int surveyThreshold = 70;
    private BigDecimal minMonthlyUnits = new BigDecimal("250");
    private BigDecimal roofAreaThresholdSqft = new BigDecimal("300");   // 3 kW x 100 sq ft/kW
    private Set<RoofType> suitableRoofTypes = EnumSet.of(RoofType.RCC_FLAT, RoofType.TIN_SHEET, RoofType.TILED);
    private double minBillConfidence = 0.80;

    private int weightOwner = 25;
    private int weightRoofType = 15;
    private int weightRoofArea = 15;
    private int weightUnits = 25;
    private int weightPincode = 10;
    private int weightBillConfidence = 10;

    public QualificationRules toRules() {
        return new QualificationRules(
                ruleVersion, surveyThreshold, minMonthlyUnits, roofAreaThresholdSqft,
                Set.copyOf(suitableRoofTypes),
                minBillConfidence,
                weightOwner, weightRoofType, weightRoofArea, weightUnits, weightPincode, weightBillConfidence);
    }
}