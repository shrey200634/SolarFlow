package com.solarFlow.solarFlow.solar.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;
import com.solarFlow.solarFlow.solar.service.SolarSizingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{leadId}/recommendations")
@RequiredArgsConstructor
public class SolarSizingController {

    private final SolarSizingService service;

    @GetMapping
    public ResponseEntity<SolarRecommendationDTO> recommend(
            @PathVariable UUID leadId,
            @AuthenticationPrincipal AuthUser actor
    ) {
        return ResponseEntity.ok(service.recommend(leadId, actor));
    }
}