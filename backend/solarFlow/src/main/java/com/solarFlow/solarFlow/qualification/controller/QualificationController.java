package com.solarFlow.solarFlow.qualification.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.qualification.service.QualificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{leadId}/qualification")
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService service;

    // Re-run scoring (staff only). Appends a new result; it never overwrites the old one.
    @PostMapping
    @PreAuthorize("hasAnyRole('SALES','ADMIN')")
    public ResponseEntity<QualificationResultDTO> run(@PathVariable UUID leadId,
                                                      @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.runManually(leadId, me));
    }

    // Latest result. Homeowners can read only their own leads (the service returns 404 otherwise).
    @GetMapping
    public ResponseEntity<QualificationResultDTO> latest(@PathVariable UUID leadId,
                                                         @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.getLatest(leadId, me));
    }
}