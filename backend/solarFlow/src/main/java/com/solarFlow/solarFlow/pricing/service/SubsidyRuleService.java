
package com.solarFlow.solarFlow.pricing.service;

import com.solarFlow.solarFlow.pricing.domain.SolarSubsidyRule;
import com.solarFlow.solarFlow.pricing.dto.CreateSubsidyRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.SubsidyRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.repo.SolarSubsidyRuleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubsidyRuleService {

    private final SolarSubsidyRuleRepository repository;

    @Transactional
    public SubsidyRuleResponseDTO create(CreateSubsidyRuleRequestDTO request) {

        validateRequest(request);

        boolean overlapping = repository.existsActiveOverlap(
                request.effectiveFrom(),
                request.effectiveTo()
        );

        if (overlapping) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active subsidy rule already exists for this date range"
            );
        }

        SolarSubsidyRule rule = new SolarSubsidyRule();

        rule.setSubsidyPerKwFirstTwoKw(
                request.subsidyPerKwFirstTwoKw()
        );

        rule.setSubsidyForThirdKw(
                request.subsidyForThirdKw()
        );

        rule.setMaximumSubsidy(
                request.maximumSubsidy()
        );

        rule.setEffectiveFrom(request.effectiveFrom());
        rule.setEffectiveTo(request.effectiveTo());
        rule.setEstimated(request.estimated());
        rule.setActive(true);

        return toResponse(repository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<SubsidyRuleResponseDTO> getAll() {

        return repository.findAllByOrderByEffectiveFromDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateRequest(CreateSubsidyRuleRequestDTO request) {

        if (request.effectiveTo() != null
                && request.effectiveTo().isBefore(request.effectiveFrom())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Effective to date cannot be before effective from date"
            );
        }

        if (request.maximumSubsidy().compareTo(
                request.subsidyPerKwFirstTwoKw()
                        .multiply(new java.math.BigDecimal("2"))
                        .add(request.subsidyForThirdKw())
        ) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum subsidy cannot be lower than the calculated subsidy for 3 kW"
            );
        }
    }

    private SubsidyRuleResponseDTO toResponse(SolarSubsidyRule rule) {

        return new SubsidyRuleResponseDTO(
                rule.getId(),
                rule.getSubsidyPerKwFirstTwoKw(),
                rule.getSubsidyForThirdKw(),
                rule.getMaximumSubsidy(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo(),
                rule.isActive(),
                rule.isEstimated()
        );
    }
}
