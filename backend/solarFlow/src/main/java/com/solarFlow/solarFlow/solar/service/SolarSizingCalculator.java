package com.solarFlow.solarFlow.solar.service;

import com.solarFlow.solarFlow.solar.dto.SolarSizingCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolarSizingCalculator {

    private final SolarSizingProperties properties;

    public SolarSizingCalculation calculate(
            BigDecimal monthlyUnits,
            BigDecimal roofAreaSqft,
            BigDecimal sanctionedLoadKw
    ) {

        if (monthlyUnits == null || monthlyUnits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly units must be positive");
        }

        if (roofAreaSqft == null || roofAreaSqft.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Roof area must be positive");
        }

        if (sanctionedLoadKw != null
                && sanctionedLoadKw.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sanctioned load must be positive");
        }

        // 1. Consumption-based capacity.
        BigDecimal annualConsumption = monthlyUnits.multiply(BigDecimal.valueOf(12));

        BigDecimal usageLimit = annualConsumption.divide(
                properties.getAnnualGenerationPerKw(),
                4,
                RoundingMode.DOWN
        );

        // 2. Roof-based capacity.
        BigDecimal roofLimit = roofAreaSqft.divide(
                properties.getRoofAreaPerKw(),
                4,
                RoundingMode.DOWN
        );

        // 3. Find the maximum technically permitted capacity.
        List<BigDecimal> physicalLimits = new ArrayList<>();
        physicalLimits.add(roofLimit);

        if (sanctionedLoadKw != null) {
            physicalLimits.add(sanctionedLoadKw);
        }

        BigDecimal maximumCapacity = physicalLimits.stream()
                .min(BigDecimal::compareTo)
                .orElseThrow();

        // 4. Recommended capacity is constrained by all known limits.
        List<BigDecimal> allLimits = new ArrayList<>(physicalLimits);
        allLimits.add(usageLimit);

        BigDecimal rawRecommended = allLimits.stream()
                .min(BigDecimal::compareTo)
                .orElseThrow();

        BigDecimal recommended = roundDownToStep(rawRecommended);

        // 5. Economy option: approximately 70% of recommended capacity.
        BigDecimal economy = roundDownToStep(
                recommended.multiply(properties.getEconomyFactor())
        );

        // 6. Maximum option: roof and sanctioned-load limit.
        BigDecimal maximum = roundDownToStep(maximumCapacity);

        // 7. Explain what constrained the recommended capacity.
        String limitingFactor = determineLimitingFactor(
                usageLimit,
                roofLimit,
                sanctionedLoadKw
        );

        // Standard rooftop system minimum for this prototype: 1 kW.
        boolean viable = recommended.compareTo(new BigDecimal("1.0")) >= 0;

        return new SolarSizingCalculation(
                usageLimit.setScale(2, RoundingMode.HALF_UP),
                roofLimit.setScale(2, RoundingMode.HALF_UP),
                sanctionedLoadKw == null
                        ? null
                        : sanctionedLoadKw.setScale(2, RoundingMode.HALF_UP),
                recommended,
                economy,
                maximum,
                limitingFactor,
                viable
        );
    }

    private BigDecimal roundDownToStep(BigDecimal capacity) {

        return capacity
                .divide(properties.getCapacityStep(), 0, RoundingMode.DOWN)
                .multiply(properties.getCapacityStep())
                .setScale(1, RoundingMode.UNNECESSARY);
    }

    private String determineLimitingFactor(
            BigDecimal usageLimit,
            BigDecimal roofLimit,
            BigDecimal sanctionedLoadKw
    ) {

        BigDecimal minimum = usageLimit.min(roofLimit);

        if (sanctionedLoadKw != null) {
            minimum = minimum.min(sanctionedLoadKw);
        }

        if (minimum.compareTo(usageLimit) == 0) {
            return "CONSUMPTION";
        }

        if (minimum.compareTo(roofLimit) == 0) {
            return "ROOF_AREA";
        }

        return "SANCTIONED_LOAD";
    }
}