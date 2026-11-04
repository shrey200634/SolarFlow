package com.solarFlow.solarFlow.pricing.service;

import com.solarFlow.solarFlow.pricing.domain.SolarPricingRule;
import com.solarFlow.solarFlow.pricing.dto.CreatePricingRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.PricingRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.repo.SolarPricingRuleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final SolarPricingRuleRepository repository;

    @Transactional
    public PricingRuleResponseDTO create(CreatePricingRuleRequestDTO request) {

        validateDates(request);

        boolean overlapping = repository.existsActiveOverlap(
                request.effectiveFrom(),
                request.effectiveTo()
        );

        if (overlapping) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active pricing rule already exists for this date range"
            );
        }

        SolarPricingRule rule = new SolarPricingRule();
        rule.setPricePerKw(request.pricePerKw());
        rule.setElectricityTariffPerUnit(request.electricityTariffPerUnit());
        rule.setEffectiveFrom(request.effectiveFrom());
        rule.setEffectiveTo(request.effectiveTo());
        rule.setActive(true);

        return toResponse(repository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponseDTO> getAll() {

        return repository.findAllByOrderByEffectiveFromDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateDates(CreatePricingRuleRequestDTO request) {

        if (request.effectiveTo() != null
                && request.effectiveTo().isBefore(request.effectiveFrom())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Effective to date cannot be before effective from date"
            );
        }
    }

    private PricingRuleResponseDTO toResponse(SolarPricingRule rule) {

        return new PricingRuleResponseDTO(
                rule.getId(),
                rule.getPricePerKw(),
                rule.getElectricityTariffPerUnit(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo(),
                rule.isActive()
        );
    }
}