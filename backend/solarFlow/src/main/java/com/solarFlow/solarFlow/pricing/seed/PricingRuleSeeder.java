package com.solarFlow.solarFlow.pricing.seed;

import com.solarFlow.solarFlow.pricing.domain.SolarPricingRule;
import com.solarFlow.solarFlow.pricing.domain.SolarSubsidyRule;
import com.solarFlow.solarFlow.pricing.repo.SolarPricingRuleRepository;
import com.solarFlow.solarFlow.pricing.repo.SolarSubsidyRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Hypothetical placeholder rules so a fresh database can issue quotations. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PricingRuleSeeder implements ApplicationRunner {

    private final SolarPricingRuleRepository pricingRules;
    private final SolarSubsidyRuleRepository subsidyRules;

    @Override
    public void run(ApplicationArguments args) {

        if (pricingRules.count() == 0) {
            SolarPricingRule rule = new SolarPricingRule();
            rule.setPricePerKw(new BigDecimal("60000.00"));
            rule.setElectricityTariffPerUnit(new BigDecimal("8.00"));
            rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
            rule.setActive(true);
            pricingRules.save(rule);
            log.info("Seeded hypothetical pricing rule (INR 60,000/kW, tariff 8/unit).");
        }

        if (subsidyRules.count() == 0) {
            SolarSubsidyRule rule = new SolarSubsidyRule();
            rule.setSubsidyPerKwFirstTwoKw(new BigDecimal("30000.00"));
            rule.setSubsidyForThirdKw(new BigDecimal("18000.00"));
            rule.setMaximumSubsidy(new BigDecimal("78000.00"));
            rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
            rule.setActive(true);
            rule.setEstimated(true);
            subsidyRules.save(rule);
            log.info("Seeded unverified subsidy rule (estimate).");
        }
    }
}