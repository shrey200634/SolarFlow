package com.solarFlow.solarFlow.solar;

import com.solarFlow.solarFlow.solar.dto.SolarSizingCalculation;
import com.solarFlow.solarFlow.solar.service.SolarSizingCalculator;
import com.solarFlow.solarFlow.solar.service.SolarSizingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SolarSizingCalculatorTest {

    private SolarSizingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SolarSizingCalculator(new SolarSizingProperties());
    }

    @Test
    void calculatesRecommendedCapacityFromConsumption() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("340"),
                new BigDecimal("450"),
                new BigDecimal("3.0")
        );

        assertEquals(new BigDecimal("2.5"), result.recommendedCapacityKw());
        assertEquals("CONSUMPTION", result.limitingFactor());
        assertTrue(result.viable());
    }

    @Test
    void calculatesThreeKwFor350MonthlyUnits() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("350"),
                new BigDecimal("450"),
                new BigDecimal("5.0")
        );

        assertEquals(new BigDecimal("3.0"), result.recommendedCapacityKw());
    }

    @Test
    void roofAreaCanLimitRecommendedCapacity() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("500"),
                new BigDecimal("200"),
                new BigDecimal("5.0")
        );

        assertEquals(new BigDecimal("2.0"), result.recommendedCapacityKw());
        assertEquals("ROOF_AREA", result.limitingFactor());
    }

    @Test
    void sanctionedLoadCanLimitRecommendedCapacity() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("500"),
                new BigDecimal("500"),
                new BigDecimal("2.0")
        );

        assertEquals(new BigDecimal("2.0"), result.recommendedCapacityKw());
        assertEquals("SANCTIONED_LOAD", result.limitingFactor());
    }

    @Test
    void calculatesEconomyAndMaximumOptions() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("350"),
                new BigDecimal("500"),
                new BigDecimal("5.0")
        );

        assertEquals(new BigDecimal("3.0"), result.recommendedCapacityKw());
        assertEquals(new BigDecimal("2.0"), result.economyCapacityKw());
        assertEquals(new BigDecimal("5.0"), result.maximumCapacityKw());
    }

    @Test
    void roundsCapacityDownToNearestHalfKw() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("340"),
                new BigDecimal("450"),
                new BigDecimal("2.8")
        );

        assertEquals(new BigDecimal("2.5"), result.recommendedCapacityKw());
    }

    @Test
    void marksCapacityBelowOneKwAsNotViable() {

        SolarSizingCalculation result = calculator.calculate(
                new BigDecimal("85"),
                new BigDecimal("450"),
                new BigDecimal("3.0")
        );

        assertEquals(new BigDecimal("0.5"), result.recommendedCapacityKw());
        assertFalse(result.viable());
    }

    @Test
    void rejectsInvalidMonthlyUnits() {

        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(
                        BigDecimal.ZERO,
                        new BigDecimal("450"),
                        new BigDecimal("3.0")
                )
        );
    }

    @Test
    void rejectsInvalidRoofArea() {

        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(
                        new BigDecimal("350"),
                        BigDecimal.ZERO,
                        new BigDecimal("3.0")
                )
        );
    }
}