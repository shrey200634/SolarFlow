package com.solarFlow.solarFlow.lead.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.common.PageResponse;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES')")
    public ResponseEntity<LeadResponseDTO> create(@Valid @RequestBody CreateLeadRequestDTO req,
                                                  @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createLead(req, me));
    }

    @GetMapping
    public ResponseEntity<PageResponse<LeadResponseDTO>> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(me, status, pincode, assignee, q, page, size));
    }

    // Counts per status for the sales overview.
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SALES','ADMIN')")
    public ResponseEntity<LeadStatsDTO> stats() {
        return ResponseEntity.ok(service.stats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponseDTO> get(@PathVariable UUID id,
                                               @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.get(id, me));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES','ADMIN')")
    public ResponseEntity<LeadResponseDTO> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateLeadRequestDTO req) {
        return ResponseEntity.ok(service.update(id, req));
    }
}