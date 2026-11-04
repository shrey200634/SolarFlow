
package com.solarFlow.solarFlow.pricing.service;

import com.solarFlow.solarFlow.pricing.domain.SolarPricingRule;
import com.solarFlow.solarFlow.pricing.domain.SolarSubsidyRule;
import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.pricing.repo.SolarPricingRuleRepository;
import com.solarFlow.solarFlow.pricing.repo.SolarSubsidyRuleRepository;
import com.solarFlow.solarFlow.solar.service.SolarSizingProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingCalculationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final int MONEY_SCALE = 2;

    private final SolarPricingRuleRepository pricingRepository;
    private final SolarSubsidyRuleRepository subsidyRepository;
    private final SolarSizingProperties solarSizingProperties;

    @Transactional(readOnly = true)
    public PricingCalculationDTO calculate(BigDecimal capacityKw) {

        if (capacityKw == null || capacityKw.compareTo(ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "System capacity must be greater than zero"
            );
        }

        LocalDate calculationDate = LocalDate.now();

        // Resolve and validate pricing first.
        SolarPricingRule pricingRule =
                getApplicablePricingRule(calculationDate);

        validatePricingRule(pricingRule);

        // Resolve and validate subsidy only after pricing is valid.
        SolarSubsidyRule subsidyRule =
                getApplicableSubsidyRule(calculationDate);

        validateSubsidyRule(subsidyRule);

        BigDecimal generationPerKw =
                solarSizingProperties.getAnnualGenerationPerKw();

        if (generationPerKw == null
                || generationPerKw.compareTo(ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Annual generation configuration is invalid"
            );
        }

        BigDecimal grossCost = money(
                capacityKw.multiply(pricingRule.getPricePerKw())
        );

        BigDecimal subsidy = calculateSubsidy(
                capacityKw,
                subsidyRule
        );

        BigDecimal netCost = money(
                grossCost.subtract(subsidy).max(ZERO)
        );

        BigDecimal annualGeneration = capacityKw
                .multiply(generationPerKw)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal annualSavings = money(
                annualGeneration.multiply(
                        pricingRule.getElectricityTariffPerUnit()
                )
        );

        BigDecimal paybackYears = annualSavings.compareTo(ZERO) > 0
                ? netCost.divide(annualSavings, 2, RoundingMode.HALF_UP)
                : null;

        return new PricingCalculationDTO(
                capacityKw,
                pricingRule.getPricePerKw(),
                grossCost,
                subsidy,
                netCost,
                annualGeneration,
                pricingRule.getElectricityTariffPerUnit(),
                annualSavings,
                paybackYears,
                subsidyRule.isEstimated(),
                calculationDate,
                pricingRule.getId(),
                subsidyRule.getId()
        );
    }

    private SolarPricingRule getApplicablePricingRule(LocalDate date) {

        List<SolarPricingRule> rules =
                pricingRepository.findApplicableRules(date);

        if (rules.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No applicable solar pricing rule found"
            );
        }

        if (rules.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple active pricing rules overlap for this date"
            );
        }

        return rules.get(0);
    }

    private SolarSubsidyRule getApplicableSubsidyRule(LocalDate date) {

        List<SolarSubsidyRule> rules =
                subsidyRepository.findApplicableRules(date);

        if (rules.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No applicable subsidy rule found"
            );
        }

        if (rules.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple active subsidy rules overlap for this date"
            );
        }

        return rules.get(0);
    }

    private BigDecimal calculateSubsidy(
            BigDecimal capacityKw,
            SolarSubsidyRule rule
    ) {

        BigDecimal firstTwoKw = capacityKw.min(TWO);

        BigDecimal thirdKw = capacityKw
                .subtract(TWO)
                .max(ZERO)
                .min(BigDecimal.ONE);

        BigDecimal subsidy = firstTwoKw
                .multiply(rule.getSubsidyPerKwFirstTwoKw())
                .add(
                        thirdKw.multiply(rule.getSubsidyForThirdKw())
                );

        return money(subsidy.min(rule.getMaximumSubsidy()));
    }

    private void validatePricingRule(SolarPricingRule rule) {

        if (rule.getPricePerKw() == null
                || rule.getPricePerKw().compareTo(ZERO) <= 0
                || rule.getElectricityTariffPerUnit() == null
                || rule.getElectricityTariffPerUnit().compareTo(ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pricing rule contains invalid financial values"
            );
        }
    }

    private void validateSubsidyRule(SolarSubsidyRule rule) {

        if (rule.getSubsidyPerKwFirstTwoKw() == null
                || rule.getSubsidyPerKwFirstTwoKw().compareTo(ZERO) < 0
                || rule.getSubsidyForThirdKw() == null
                || rule.getSubsidyForThirdKw().compareTo(ZERO) < 0
                || rule.getMaximumSubsidy() == null
                || rule.getMaximumSubsidy().compareTo(ZERO) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subsidy rule contains invalid financial values"
            );
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
