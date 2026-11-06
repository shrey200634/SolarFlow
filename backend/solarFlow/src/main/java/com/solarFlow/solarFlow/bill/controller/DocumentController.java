package com.solarFlow.solarFlow.bill.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.service.BillConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES','ADMIN')")
    public ResponseEntity<ExtractionResponseDTO> getExtraction(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.getExtraction(documentId, me));
    }

    @PostMapping("/{documentId}/extraction/confirm")
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES','ADMIN')")
    public ResponseEntity<ExtractionResponseDTO> confirm(
            @PathVariable UUID documentId,
            @Valid @RequestBody ConfirmBillRequestDTO request,
            @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.confirm(documentId, request, me));
    }

    // The uploaded bill itself. Same ownership rule as the other document endpoints
    // (a homeowner gets 404 for someone else's document). The storage key is never exposed.
    @GetMapping("/{documentId}/file")
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES','ADMIN')")
    public ResponseEntity<byte[]> file(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthUser me) {
        BillConfirmationService.StoredFile f = service.readFile(documentId, me);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(f.data());
    }
}