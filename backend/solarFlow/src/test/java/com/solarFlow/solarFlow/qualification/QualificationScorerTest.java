package com.solarFlow.solarFlow.qualification;

import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.lead.domain.RoofType;
import com.solarFlow.solarFlow.qualification.domain.NextAction;
import com.solarFlow.solarFlow.qualification.dto.QualificationInput;
import com.solarFlow.solarFlow.qualification.dto.QualificationOutcome;
import com.solarFlow.solarFlow.qualification.dto.QualificationRules;
import com.solarFlow.solarFlow.qualification.service.QualificationScorer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class QualificationScorerTest {

    private static final QualificationRules RULES = new QualificationRules(
            "v1", 70, new BigDecimal("250"), new BigDecimal("300"),
            EnumSet.of(RoofType.RCC_FLAT, RoofType.TIN_SHEET, RoofType.TILED),
            0.80, 25, 15, 15, 25, 10, 10);

    private final QualificationScorer scorer = new QualificationScorer(RULES);

    private static QualificationInput in(Ownership o, RoofType r, String area, boolean pin, String units, Double conf) {
        return new QualificationInput(o, r, new BigDecimal(area), pin, new BigDecimal(units), conf);
    }

    private static boolean gateTriggered(QualificationOutcome o, String code) {
        return o.gates().stream().anyMatch(g -> g.code().equals(code) && g.triggered());
    }

    // ---- the three client sample leads ----

    @Test
    void sampleLeadPrashantOwnerWithGoodBillGetsSurvey() {
        // Owner, 340 units/month, good roof, serviceable pincode, confident AI read
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "340", 0.92));
        assertEquals(100, o.score());
        assertEquals(NextAction.SURVEY_RECOMMENDED, o.nextAction());
    }

    @Test
    void sampleLeadRohitTenantGetsSalesCall() {
        QualificationOutcome o = scorer.score(in(Ownership.RENTED, RoofType.RCC_FLAT, "450", true, "210", 0.92));
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
        assertTrue(gateTriggered(o, "NOT_PROPERTY_OWNER"));
        assertTrue(gateTriggered(o, "LOW_CONSUMPTION"));
    }

    @Test
    void sampleLeadAyeshaSmallBillGetsSalesCallEvenThoughPointsReach70() {
        // Raw points: owner 25 + roof 15 + area 15 + pincode 10 + confidence 10 = 75 (>= 70)
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "85", 0.92));
        assertEquals(75, o.score());
        assertTrue(gateTriggered(o, "LOW_CONSUMPTION"));
        assertFalse(gateTriggered(o, "NOT_PROPERTY_OWNER"));
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
    }

    // ---- gates ----

    @Test
    void tenantWithHighConsumptionStillGetsSalesCall() {
        // 0 + 15 + 15 + 25 + 10 + 10 = 75, but a tenant needs owner consent first
        QualificationOutcome o = scorer.score(in(Ownership.RENTED, RoofType.RCC_FLAT, "450", true, "400", 0.92));
        assertEquals(75, o.score());
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
    }

    @Test
    void leasedPropertyIsTreatedAsNotOwner() {
        QualificationOutcome o = scorer.score(in(Ownership.LEASED, RoofType.RCC_FLAT, "450", true, "400", 0.92));
        assertTrue(gateTriggered(o, "NOT_PROPERTY_OWNER"));
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
    }

    @Test
    void exactlyMinimumUnitsPassesTheGate() {
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "250", 0.92));
        assertFalse(gateTriggered(o, "LOW_CONSUMPTION"));
        assertEquals(NextAction.SURVEY_RECOMMENDED, o.nextAction());
    }

    @Test
    void justBelowMinimumUnitsTripsTheGate() {
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "249.9", 0.92));
        assertTrue(gateTriggered(o, "LOW_CONSUMPTION"));
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
    }

    // ---- threshold ----

    @Test
    void scoreOfExactly70WithNoGateGetsSurvey() {
        // owner 25 + units 25 + pincode 10 + confidence 10 = 70 (roof type and area both fail)
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.ASBESTOS, "100", true, "300", 0.90));
        assertEquals(70, o.score());
        assertEquals(NextAction.SURVEY_RECOMMENDED, o.nextAction());
    }

    @Test
    void scoreBelow70WithNoGateGetsSalesCallNotRejection() {
        // owner 25 + units 25 + pincode 10 = 60
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.OTHER, "100", true, "300", null));
        assertEquals(60, o.score());
        assertFalse(gateTriggered(o, "NOT_PROPERTY_OWNER"));
        assertFalse(gateTriggered(o, "LOW_CONSUMPTION"));
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
    }

    @Test
    void worstPossibleLeadStillOnlyGetsASalesCall() {
        QualificationOutcome o = scorer.score(in(Ownership.RENTED, RoofType.OTHER, "50", false, "10", null));
        assertEquals(0, o.score());
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
        assertNotNull(o.nextAction());
    }

    // ---- individual rules ----

    @Test
    void roofTypeRuleFollowsTheConfiguredList() {
        assertEquals(15, pointsFor(in(Ownership.OWNED, RoofType.TIN_SHEET, "450", true, "340", 0.9), "ROOF_TYPE"));
        assertEquals(15, pointsFor(in(Ownership.OWNED, RoofType.TILED, "450", true, "340", 0.9), "ROOF_TYPE"));
        assertEquals(0, pointsFor(in(Ownership.OWNED, RoofType.ASBESTOS, "450", true, "340", 0.9), "ROOF_TYPE"));
        assertEquals(0, pointsFor(in(Ownership.OWNED, RoofType.OTHER, "450", true, "340", 0.9), "ROOF_TYPE"));
    }

    @Test
    void roofAreaBoundaryIs300SqFt() {
        assertEquals(15, pointsFor(in(Ownership.OWNED, RoofType.RCC_FLAT, "300", true, "340", 0.9), "ROOF_AREA"));
        assertEquals(0, pointsFor(in(Ownership.OWNED, RoofType.RCC_FLAT, "299.9", true, "340", 0.9), "ROOF_AREA"));
    }

    @Test
    void billConfidenceRule() {
        assertEquals(10, pointsFor(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "340", 0.80), "BILL_CONFIDENCE"));
        assertEquals(0, pointsFor(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "340", 0.79), "BILL_CONFIDENCE"));
        assertEquals(0, pointsFor(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "340", null), "BILL_CONFIDENCE"));
    }

    @Test
    void unserviceablePincodeLosesItsPointsButIsNotAGate() {
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", false, "340", 0.92));
        assertEquals(90, o.score());
        assertEquals(NextAction.SURVEY_RECOMMENDED, o.nextAction());
    }

    // ---- explainability and determinism ----

    @Test
    void everyRuleProducesAReasonWithText() {
        QualificationOutcome o = scorer.score(in(Ownership.OWNED, RoofType.RCC_FLAT, "450", true, "340", 0.92));
        assertEquals(6, o.reasons().size());
        assertTrue(o.reasons().stream().allMatch(r -> r.text() != null && !r.text().isBlank()));
        assertEquals(100, o.reasons().stream().mapToInt(r -> r.maxPoints()).sum());
    }

    @Test
    void sameInputsGiveSameOutcome() {
        QualificationInput i = in(Ownership.OWNED, RoofType.TILED, "320", true, "280", 0.85);
        assertEquals(scorer.score(i), scorer.score(i));
    }

    @Test
    void customWeightsAndThresholdAreRespected() {
        QualificationRules strict = new QualificationRules("v2", 95, new BigDecimal("250"), new BigDecimal("300"),
                EnumSet.of(RoofType.RCC_FLAT), 0.80, 25, 15, 15, 25, 10, 10);
        QualificationOutcome o = new QualificationScorer(strict)
                .score(in(Ownership.OWNED, RoofType.TILED, "450", true, "340", 0.92));
        assertEquals(85, o.score());
        assertEquals(NextAction.SALES_CALL_PENDING, o.nextAction());
    }

    private int pointsFor(QualificationInput in, String code) {
        return scorer.score(in).reasons().stream()
                .filter(r -> r.code().equals(code)).findFirst().orElseThrow().points();
    }
}