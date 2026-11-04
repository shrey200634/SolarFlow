package com.solarFlow.solarFlow.quotation.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.quotation.dto.CreateQuotationRequestDTO;
import com.solarFlow.solarFlow.quotation.dto.QuotationResponseDTO;
import com.solarFlow.solarFlow.quotation.service.QuotationPdfService;
import com.solarFlow.solarFlow.quotation.service.QuotationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;
    private final QuotationPdfService quotationPdfService;

    @PostMapping("/api/v1/leads/{leadId}/quotations")
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
    public ResponseEntity<QuotationResponseDTO> create(
            @PathVariable UUID leadId,
            @Valid @RequestBody CreateQuotationRequestDTO request,
            @AuthenticationPrincipal AuthUser actor
    ) {
        return ResponseEntity.ok(
                quotationService.create(leadId, request, actor)
        );
    }

    @GetMapping("/api/v1/quotations/{quotationId}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'SALES', 'ADMIN')")
    public ResponseEntity<QuotationResponseDTO> get(
            @PathVariable UUID quotationId,
            @AuthenticationPrincipal AuthUser actor
    ) {
        return ResponseEntity.ok(
                quotationService.get(quotationId, actor)
        );
    }

    @GetMapping("/api/v1/quotations/{quotationId}/pdf")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'SALES', 'ADMIN')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID quotationId,
            @AuthenticationPrincipal AuthUser actor
    ) {

        byte[] pdf = quotationService.downloadPdf(quotationId, actor);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("solarflow-quotation-" + quotationId + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    @PostMapping("/api/v1/quotations/{quotationId}/accept")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<QuotationResponseDTO> accept(
            @PathVariable UUID quotationId,
            @AuthenticationPrincipal AuthUser actor
    ) {
        return ResponseEntity.ok(
                quotationService.accept(quotationId, actor)
        );
    }
}