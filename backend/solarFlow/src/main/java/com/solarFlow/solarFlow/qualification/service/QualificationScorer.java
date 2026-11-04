package com.solarFlow.solarFlow.qualification.service;

import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.qualification.domain.NextAction;
import com.solarFlow.solarFlow.qualification.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public final class QualificationScorer {

    private final QualificationRules rules;

    public QualificationOutcome score(QualificationInput in) {
        List<QualificationReason> reasons = new ArrayList<>();

        boolean owner = in.ownership() == Ownership.OWNED;
        reasons.add(reason("PROPERTY_OWNER", owner, rules.weightOwner(),
                owner ? "Applicant owns the property"
                        : "Applicant does not own the property (" + in.ownership() + ")"));

        boolean roofSuitable = rules.suitableRoofTypes().contains(in.roofType());
        reasons.add(reason("ROOF_TYPE", roofSuitable, rules.weightRoofType(),
                roofSuitable ? "Roof type " + in.roofType() + " is suitable for solar"
                        : "Roof type " + in.roofType() + " is not in the suitable list"));

        boolean roofBigEnough = in.roofAreaSqft().compareTo(rules.roofAreaThresholdSqft()) >= 0;
        reasons.add(reason("ROOF_AREA", roofBigEnough, rules.weightRoofArea(),
                roofBigEnough
                        ? "Roof area " + num(in.roofAreaSqft()) + " sq ft is enough for a 3 kW system (needs "
                        + num(rules.roofAreaThresholdSqft()) + ")"
                        : "Roof area " + num(in.roofAreaSqft()) + " sq ft is below the "
                        + num(rules.roofAreaThresholdSqft()) + " sq ft needed for 3 kW"));

        boolean unitsOk = in.monthlyUnits().compareTo(rules.minMonthlyUnits()) >= 0;
        reasons.add(reason("MONTHLY_UNITS", unitsOk, rules.weightUnits(),
                unitsOk ? "Monthly consumption " + num(in.monthlyUnits()) + " units meets the "
                        + num(rules.minMonthlyUnits()) + "-unit minimum"
                        : "Monthly consumption " + num(in.monthlyUnits()) + " units is below the "
                        + num(rules.minMonthlyUnits()) + "-unit minimum"));

        reasons.add(reason("PINCODE_SERVICEABLE", in.pincodeServiceable(), rules.weightPincode(),
                in.pincodeServiceable() ? "Pincode is in the serviceable area"
                        : "Pincode is outside the serviceable area"));

        boolean confident = in.billConfidence() != null && in.billConfidence() >= rules.minBillConfidence();
        String confText;
        if (in.billConfidence() == null) {
            confText = "Bill details were entered manually, so there is no AI confidence to rely on";
        } else if (confident) {
            confText = "Bill was read with high confidence (" + in.billConfidence() + ")";
        } else {
            confText = "Bill was read with low confidence (" + in.billConfidence() + ", needs "
                    + rules.minBillConfidence() + ")";
        }
        reasons.add(reason("BILL_CONFIDENCE", confident, rules.weightBillConfidence(), confText));

        int score = reasons.stream().mapToInt(QualificationReason::points).sum();

        List<QualificationGate> gates = new ArrayList<>();
        gates.add(new QualificationGate("NOT_PROPERTY_OWNER", !owner,
                owner ? "Applicant owns the property"
                        : "Applicant is not the property owner; a sales call is needed to check owner consent before any survey"));
        gates.add(new QualificationGate("LOW_CONSUMPTION", !unitsOk,
                unitsOk ? "Consumption is at or above the minimum"
                        : "Consumption is below " + num(rules.minMonthlyUnits())
                        + " units/month; a short call can check whether solar still makes sense"));

        boolean anyGate = gates.stream().anyMatch(QualificationGate::triggered);
        NextAction action = (score >= rules.surveyThreshold() && !anyGate)
                ? NextAction.SURVEY_RECOMMENDED
                : NextAction.SALES_CALL_PENDING;

        return new QualificationOutcome(score, action, List.copyOf(reasons), List.copyOf(gates));
    }

    private static QualificationReason reason(String code, boolean passed, int weight, String text) {
        return new QualificationReason(code, passed ? weight : 0, weight, passed, text);
    }

    private static String num(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}