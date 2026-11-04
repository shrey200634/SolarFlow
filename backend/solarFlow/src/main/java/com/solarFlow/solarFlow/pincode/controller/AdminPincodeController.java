package com.solarFlow.solarFlow.pincode.controller;

import com.solarFlow.solarFlow.pincode.service.PincodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pincodes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPincodeController {

    private final PincodeService service;

    @GetMapping
    public ResponseEntity<List<PincodeResponseDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<PincodeResponseDTO> add(@Valid @RequestBody PincodeRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(req));
    }

    // Activate or deactivate a pincode (rows are never deleted).
    @PatchMapping("/{id}")
    public ResponseEntity<PincodeResponseDTO> setActive(@PathVariable UUID id,
                                                        @Valid @RequestBody PincodeStatusRequestDTO req) {
        return ResponseEntity.ok(service.setActive(id, req.active()));
    }
}