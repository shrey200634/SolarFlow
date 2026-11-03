package com.solarFlow.solarFlow.bill.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.service.BillConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final BillConfirmationService service;

    @GetMapping("/{documentId}/extraction")
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES')")
    public ResponseEntity<ExtractionResponseDTO> getExtraction(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthUser me
    ) {

        return ResponseEntity.ok(
                service.getExtraction(documentId, me)
        );
    }

    @PostMapping("/{documentId}/extraction/confirm")
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES')")
    public ResponseEntity<ExtractionResponseDTO> confirm(
            @PathVariable UUID documentId,
            @Valid @RequestBody ConfirmBillRequestDTO request,
            @AuthenticationPrincipal AuthUser me
    ) {

        return ResponseEntity.ok(
                service.confirm(documentId, request, me)
        );
    }
}