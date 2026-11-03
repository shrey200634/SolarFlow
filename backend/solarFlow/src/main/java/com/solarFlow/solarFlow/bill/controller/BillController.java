package com.solarFlow.solarFlow.bill.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.service.BillDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{leadId}/documents")
@RequiredArgsConstructor
public class BillController {

    private final BillDocumentService service;

    // 202 Accepted: extraction will be queued here on Day 3
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOMEOWNER','SALES')")
    public ResponseEntity<DocumentResponseDTO> upload(@PathVariable UUID leadId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.accepted().body(service.upload(leadId, file, me));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> list(@PathVariable UUID leadId,
                                                          @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(service.listForLead(leadId, me));
    }
}