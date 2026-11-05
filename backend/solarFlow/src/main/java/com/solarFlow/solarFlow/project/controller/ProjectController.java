package com.solarFlow.solarFlow.project.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    // Staff create a project from an accepted quotation, recording the advance payment.
    @PostMapping
    @PreAuthorize("hasAnyRole('SALES','ADMIN')")
    public ResponseEntity<ProjectResponseDTO> create(@Valid @RequestBody CreateProjectRequestDTO request,
                                                     @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, me));
    }

    // Staff move a project forward one stage.
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SALES','ADMIN')")
    public ResponseEntity<ProjectResponseDTO> changeStatus(@PathVariable UUID id,
                                                           @Valid @RequestBody ProjectStatusRequestDTO request,
                                                           @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.changeStatus(id, request, me));
    }

    // Staff see all projects, homeowners only their own.
    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> list(@AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.list(me));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> get(@PathVariable UUID id,
                                                  @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.get(id, me));
    }
}