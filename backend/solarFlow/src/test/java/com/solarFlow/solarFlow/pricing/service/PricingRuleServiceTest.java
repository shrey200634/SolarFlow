
package com.solarFlow.solarFlow.pricing.service;

import com.solarFlow.solarFlow.pricing.domain.SolarPricingRule;
import com.solarFlow.solarFlow.pricing.dto.CreatePricingRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.PricingRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.repo.SolarPricingRuleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingRuleServiceTest {

    @Mock
    private SolarPricingRuleRepository repository;

    @InjectMocks
    private PricingRuleService service;

    private CreatePricingRuleRequestDTO validRequest() {
        return new CreatePricingRuleRequestDTO(
                new BigDecimal("100000"),
                new BigDecimal("8"),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 10, 31)
        );
    }

    @Test
    void createsPricingRuleSuccessfully() {

        when(repository.existsActiveOverlap(any(), any()))
                .thenReturn(false);

        when(repository.save(any(SolarPricingRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PricingRuleResponseDTO result = service.create(validRequest());

        assertEquals(new BigDecimal("100000"), result.pricePerKw());
        assertEquals(new BigDecimal("8"), result.electricityTariffPerUnit());
        assertEquals(LocalDate.of(2026, 11, 1), result.effectiveFrom());
        assertEquals(LocalDate.of(2027, 10, 31), result.effectiveTo());
        assertTrue(result.active());

        verify(repository).save(any(SolarPricingRule.class));
    }

    @Test
    void rejectsOverlappingPricingRule() {

        when(repository.existsActiveOverlap(any(), any()))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(validRequest())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsEffectiveToBeforeEffectiveFrom() {

        CreatePricingRuleRequestDTO request =
                new CreatePricingRuleRequestDTO(
                        new BigDecimal("100000"),
                        new BigDecimal("8"),
                        LocalDate.of(2027, 10, 31),
                        LocalDate.of(2026, 11, 1)
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(repository);
    }

    @Test
    void allowsOpenEndedPricingRuleWhenNoOverlapExists() {

        CreatePricingRuleRequestDTO request =
                new CreatePricingRuleRequestDTO(
                        new BigDecimal("100000"),
                        new BigDecimal("8"),
                        LocalDate.of(2026, 11, 1),
                        null
                );

        when(repository.existsActiveOverlap(any(), any()))
                .thenReturn(false);

        when(repository.save(any(SolarPricingRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PricingRuleResponseDTO result = service.create(request);

        assertNull(result.effectiveTo());
        assertTrue(result.active());
    }

    @Test
    void returnsPricingRulesOrderedByRepository() {

        SolarPricingRule rule = new SolarPricingRule();
        rule.setPricePerKw(new BigDecimal("100000"));
        rule.setElectricityTariffPerUnit(new BigDecimal("8"));
        rule.setEffectiveFrom(LocalDate.of(2026, 11, 1));
        rule.setActive(true);

        when(repository.findAllByOrderByEffectiveFromDesc())
                .thenReturn(List.of(rule));

        List<PricingRuleResponseDTO> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100000"), result.get(0).pricePerKw());
    }
}
