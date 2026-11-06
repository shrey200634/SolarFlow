
package com.solarFlow.solarFlow.pricing.controller;

import com.solarFlow.solarFlow.pricing.dto.CreateSubsidyRuleRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.RuleStatusRequestDTO;
import com.solarFlow.solarFlow.pricing.dto.SubsidyRuleResponseDTO;
import com.solarFlow.solarFlow.pricing.service.SubsidyRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subsidy-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SubsidyRuleAdminController {

    private final SubsidyRuleService subsidyRuleService;

    @PostMapping
    public SubsidyRuleResponseDTO createSubsidyRule(
            @Valid @RequestBody CreateSubsidyRuleRequestDTO request
    ) {
        return subsidyRuleService.create(request);
    }
    @PatchMapping("/{id}")
    public SubsidyRuleResponseDTO setActive(@PathVariable UUID id,
                                            @Valid @RequestBody RuleStatusRequestDTO request) {
        return subsidyRuleService.setActive(id, request.active());
    }

    @GetMapping
    public List<SubsidyRuleResponseDTO> getSubsidyRules() {
        return subsidyRuleService.getAll();
    }
}
