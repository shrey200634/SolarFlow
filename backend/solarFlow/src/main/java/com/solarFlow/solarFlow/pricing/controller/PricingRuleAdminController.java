package com.solarFlow.solarFlow.pricing.controller;

import com.solarFlow.solarFlow.pricing.dto.CreatePricingRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.PricingRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.service.PricingRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public List<PricingRuleResponseDTO> getPricingRules() {
        return pricingRuleService.getAll();
    }
}