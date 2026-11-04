
package com.solarFlow.solarFlow.pricing.service;

import com.solarFlow.solarFlow.pricing.domain.SolarSubsidyRule;
import com.solarFlow.solarFlow.pricing.dto.CreateSubsidyRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.SubsidyRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.repo.SolarSubsidyRuleRepository;

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
class SubsidyRuleServiceTest {

    @Mock
    private SolarSubsidyRuleRepository repository;

    @InjectMocks
    private SubsidyRuleService service;

    private CreateSubsidyRuleRequestDTO validRequest() {
        return new CreateSubsidyRuleRequestDTO(
                new BigDecimal("30000"),
                new BigDecimal("18000"),
                new BigDecimal("78000"),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 10, 31),
                true
        );
    }

    @Test
    void createsSubsidyRuleSuccessfully() {

        when(repository.existsActiveOverlap(any(), any()))
                .thenReturn(false);

        when(repository.save(any(SolarSubsidyRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubsidyRuleResponseDTO result = service.create(validRequest());

        assertEquals(new BigDecimal("30000"), result.subsidyPerKwFirstTwoKw());
        assertEquals(new BigDecimal("18000"), result.subsidyForThirdKw());
        assertEquals(new BigDecimal("78000"), result.maximumSubsidy());

        assertTrue(result.active());
        assertTrue(result.estimated());

        verify(repository).save(any(SolarSubsidyRule.class));
    }

    @Test
    void rejectsOverlappingSubsidyRule() {

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

        CreateSubsidyRuleRequestDTO request =
                new CreateSubsidyRuleRequestDTO(
                        new BigDecimal("30000"),
                        new BigDecimal("18000"),
                        new BigDecimal("78000"),
                        LocalDate.of(2027, 10, 31),
                        LocalDate.of(2026, 11, 1),
                        true
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMaximumSubsidyBelowThreeKwCalculatedAmount() {

        CreateSubsidyRuleRequestDTO request =
                new CreateSubsidyRuleRequestDTO(
                        new BigDecimal("30000"),
                        new BigDecimal("18000"),
                        new BigDecimal("60000"),
                        LocalDate.of(2026, 11, 1),
                        null,
                        true
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(repository);
    }

    @Test
    void allowsOpenEndedSubsidyRule() {

        CreateSubsidyRuleRequestDTO request =
                new CreateSubsidyRuleRequestDTO(
                        new BigDecimal("30000"),
                        new BigDecimal("18000"),
                        new BigDecimal("78000"),
                        LocalDate.of(2026, 11, 1),
                        null,
                        false
                );

        when(repository.existsActiveOverlap(any(), any()))
                .thenReturn(false);

        when(repository.save(any(SolarSubsidyRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubsidyRuleResponseDTO result = service.create(request);

        assertNull(result.effectiveTo());
        assertFalse(result.estimated());
    }

    @Test
    void returnsSubsidyRules() {

        SolarSubsidyRule rule = new SolarSubsidyRule();

        rule.setSubsidyPerKwFirstTwoKw(new BigDecimal("30000"));
        rule.setSubsidyForThirdKw(new BigDecimal("18000"));
        rule.setMaximumSubsidy(new BigDecimal("78000"));
        rule.setEffectiveFrom(LocalDate.of(2026, 11, 1));
        rule.setEstimated(true);
        rule.setActive(true);

        when(repository.findAllByOrderByEffectiveFromDesc())
                .thenReturn(List.of(rule));

        List<SubsidyRuleResponseDTO> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("78000"), result.get(0).maximumSubsidy());
    }
}
