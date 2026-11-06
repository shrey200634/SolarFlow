package com.solarFlow.solarFlow.pricing.controller;

import com.solarFlow.solarFlow.pricing.dto.CreatePricingRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.PricingRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.dto.RuleStatusRequestDTO;
import com.solarFlow.solarFlow.pricing.service.PricingRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pricing-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PricingRuleAdminController {

    private final PricingRuleService pricingRuleService;

    @PostMapping
    public PricingRuleResponseDTO createPricingRule(
            @Valid @RequestBody CreatePricingRuleRequestDTO request
    ) {
        return pricingRuleService.create(request);
    }

    @PatchMapping("/{id}")
    public PricingRuleResponseDTO setActive(@PathVariable UUID id,
                                            @Valid @RequestBody RuleStatusRequestDTO request) {
        return pricingRuleService.setActive(id, request.active());
    }

    @GetMapping
    public List<PricingRuleResponseDTO> getPricingRules() {
        return pricingRuleService.getAll();
    }
}