package com.solarFlow.solarFlow.bill;

import com.solarFlow.solarFlow.bill.extraction.ExtractionResult;
import com.solarFlow.solarFlow.bill.extraction.ExtractionValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionValidatorTest {

    private final ExtractionValidator v = new ExtractionValidator(
            "^[0-9]{12}$", "(?i)^LT.*", 0.80, 0.5,
            new BigDecimal("0.5"), new BigDecimal("50"), new BigDecimal("5000"),
            new BigDecimal("2"), new BigDecimal("30"));

    private static final BigDecimal DECLARED = new BigDecimal("2800");

    private ExtractionResult result(String consumer, double cConf, String tariff, String load, String units,
                                    double unitsConf, double overall) {
        return new ExtractionResult(
                new ExtractionResult.Field<>(consumer, cConf),
                new ExtractionResult.Field<>(tariff, tariff == null ? 0 : 0.95),
                new ExtractionResult.Field<>(load == null ? null : new BigDecimal(load), load == null ? 0 : 0.95),
                new ExtractionResult.Field<>(units == null ? null : new BigDecimal(units), unitsConf),
                overall, "GOOD", "test", 1, "{}");
    }

    private ExtractionResult clean() {
        return result("410012345678", 0.97, "LT-I Residential", "3.0", "350", 0.95, 0.92);
    }

    @Test
    void cleanBillHasNoReviewFields() {
        var report = v.validate(clean(), DECLARED);
        assertTrue(report.reviewFields().isEmpty());
        assertFalse(report.lowQuality());
    }

    @Test
    void shortConsumerNumberIsBadFormat() {
        var r = result("4100123", 0.97, "LT-I Residential", "3.0", "350", 0.95, 0.92);
        assertTrue(v.validate(r, DECLARED).field("consumerNumber").issues().contains("BAD_FORMAT"));
    }

    @Test
    void lowConfidenceFieldIsFlagged() {
        var r = result("410012345678", 0.97, "LT-I Residential", "3.0", "350", 0.60, 0.92);
        var f = v.validate(r, DECLARED).field("monthlyUnits");
        assertTrue(f.reviewRequired());
        assertTrue(f.issues().contains("LOW_CONFIDENCE"));
    }

    @Test
    void loadOutsideRangeIsFlagged() {
        var r = result("410012345678", 0.97, "LT-I Residential", "80", "350", 0.95, 0.92);
        assertTrue(v.validate(r, DECLARED).field("sanctionedLoadKw").issues().contains("OUT_OF_RANGE"));
    }

    @Test
    void unitsAboveMaxAreFlagged() {
        var r = result("410012345678", 0.97, "LT-I Residential", "3.0", "6000", 0.95, 0.92);
        assertTrue(v.validate(r, DECLARED).field("monthlyUnits").issues().contains("OUT_OF_RANGE"));
    }

    @Test
    void unitsThatDoNotMatchDeclaredBillAreFlagged() {
        // 2800 / 350 = 8.00 per unit -> fine (see cleanBill). Here 50000 / 100 = 500 per unit -> mismatch
        var r = result("410012345678", 0.97, "LT-I Residential", "3.0", "100", 0.95, 0.92);
        assertTrue(v.validate(r, new BigDecimal("50000")).field("monthlyUnits").issues().contains("UNITS_BILL_MISMATCH"));
        // 100 / 5000 = 0.02 per unit -> too cheap
        var r2 = result("410012345678", 0.97, "LT-I Residential", "3.0", "5000", 0.95, 0.92);
        assertTrue(v.validate(r2, new BigDecimal("100")).field("monthlyUnits").issues().contains("UNITS_BILL_MISMATCH"));
    }

    @Test
    void zeroUnitsSkipsCrossCheckWithoutDividingByZero() {
        var r = result("410012345678", 0.97, "LT-I Residential", "3.0", "0", 0.95, 0.92);
        assertFalse(v.validate(r, DECLARED).field("monthlyUnits").issues().contains("UNITS_BILL_MISMATCH"));
    }

    @Test
    void nullDeclaredBillSkipsCrossCheck() {
        assertFalse(v.validate(clean(), null).field("monthlyUnits").reviewRequired());
    }

    @Test
    void missingFieldsAreFlagged() {
        var r = result(null, 0, null, null, null, 0, 0.92);
        var report = v.validate(r, DECLARED);
        assertEquals(4, report.reviewFields().size());
        assertTrue(report.lowQuality());          // nothing was read
    }

    @Test
    void lowOverallConfidenceMeansLowQuality() {
        var r = result("410012345678", 0.97, "LT-I Residential", "3.0", "350", 0.95, 0.40);
        assertTrue(v.validate(r, DECLARED).lowQuality());
    }

    @Test
    void unknownTariffIsFlagged() {
        var r = result("410012345678", 0.97, "Banana", "3.0", "350", 0.95, 0.92);
        assertTrue(v.validate(r, DECLARED).field("tariff").issues().contains("UNKNOWN_TARIFF"));
    }
}