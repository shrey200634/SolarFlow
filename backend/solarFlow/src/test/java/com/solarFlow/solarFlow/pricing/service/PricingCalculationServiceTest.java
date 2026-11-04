
package com.solarFlow.solarFlow.pricing.service;

import com.solarFlow.solarFlow.pricing.domain.SolarPricingRule;
import com.solarFlow.solarFlow.pricing.domain.SolarSubsidyRule;
import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.pricing.repo.SolarPricingRuleRepository;
import com.solarFlow.solarFlow.pricing.repo.SolarSubsidyRuleRepository;
import com.solarFlow.solarFlow.solar.service.SolarSizingProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingCalculationServiceTest {

    @Mock
    private SolarPricingRuleRepository pricingRepository;

    @Mock
    private SolarSubsidyRuleRepository subsidyRepository;

    @Mock
    private SolarSizingProperties solarSizingProperties;

    @InjectMocks
    private PricingCalculationService service;

    private SolarPricingRule pricingRule;
    private SolarSubsidyRule subsidyRule;

    @BeforeEach
    void setUp() {

        pricingRule = new SolarPricingRule();
        pricingRule.setPricePerKw(new BigDecimal("100000"));
        pricingRule.setElectricityTariffPerUnit(new BigDecimal("8"));

        subsidyRule = new SolarSubsidyRule();
        subsidyRule.setSubsidyPerKwFirstTwoKw(new BigDecimal("30000"));
        subsidyRule.setSubsidyForThirdKw(new BigDecimal("18000"));
        subsidyRule.setMaximumSubsidy(new BigDecimal("78000"));
        subsidyRule.setEstimated(true);
    }

    private void stubValidRules() {

        when(pricingRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of(pricingRule));

        when(subsidyRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of(subsidyRule));

        when(solarSizingProperties.getAnnualGenerationPerKw())
                .thenReturn(new BigDecimal("1400"));
    }

    @Test
    void calculatesTwoAndHalfKwSystemCorrectly() {

        stubValidRules();

        PricingCalculationDTO result =
                service.calculate(new BigDecimal("2.5"));

        assertEquals(new BigDecimal("250000.00"), result.grossSystemCost());
        assertEquals(new BigDecimal("69000.00"), result.subsidyAmount());
        assertEquals(new BigDecimal("181000.00"), result.netSystemCost());

        assertEquals(new BigDecimal("3500.00"), result.annualGenerationUnits());
        assertEquals(new BigDecimal("28000.00"), result.estimatedAnnualSavings());
        assertEquals(new BigDecimal("6.46"), result.estimatedPaybackYears());

        assertTrue(result.subsidyEstimated());
    }

    @Test
    void calculatesThreeKwSystemCorrectly() {

        stubValidRules();

        PricingCalculationDTO result =
                service.calculate(new BigDecimal("3"));

        assertEquals(new BigDecimal("300000.00"), result.grossSystemCost());
        assertEquals(new BigDecimal("78000.00"), result.subsidyAmount());
        assertEquals(new BigDecimal("222000.00"), result.netSystemCost());
    }

    @Test
    void subsidyDoesNotExceedMaximumCap() {

        stubValidRules();

        PricingCalculationDTO result =
                service.calculate(new BigDecimal("5"));

        assertEquals(new BigDecimal("78000.00"), result.subsidyAmount());
    }

    @Test
    void rejectsZeroCapacity() {

        assertThrows(
                ResponseStatusException.class,
                () -> service.calculate(BigDecimal.ZERO)
        );

        verifyNoInteractions(
                pricingRepository,
                subsidyRepository,
                solarSizingProperties
        );
    }

    @Test
    void rejectsNegativeCapacity() {

        assertThrows(
                ResponseStatusException.class,
                () -> service.calculate(new BigDecimal("-1"))
        );

        verifyNoInteractions(
                pricingRepository,
                subsidyRepository,
                solarSizingProperties
        );
    }

    @Test
    void rejectsMissingPricingRule() {

        when(pricingRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of());

        assertThrows(
                ResponseStatusException.class,
                () -> service.calculate(new BigDecimal("2"))
        );

        verifyNoInteractions(subsidyRepository, solarSizingProperties);
    }

    @Test
    void rejectsMissingSubsidyRule() {

        when(pricingRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of(pricingRule));

        when(subsidyRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of());

        assertThrows(
                ResponseStatusException.class,
                () -> service.calculate(new BigDecimal("2"))
        );

        verifyNoInteractions(solarSizingProperties);
    }

    @Test
    void rejectsOverlappingPricingRules() {

        when(pricingRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of(
                        pricingRule,
                        new SolarPricingRule()
                ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.calculate(new BigDecimal("2"))
        );

        verifyNoInteractions(subsidyRepository, solarSizingProperties);
    }

    @Test
    void rejectsInvalidPricingValues() {

        pricingRule.setPricePerKw(BigDecimal.ZERO);

        when(pricingRepository.findApplicableRules(any(LocalDate.class)))
                .thenReturn(List.of(pricingRule));

        assertThrows(
                ResponseStatusException.class,
                () -> service.calculate(new BigDecimal("2"))
        );

        verifyNoInteractions(subsidyRepository, solarSizingProperties);
    }
}
