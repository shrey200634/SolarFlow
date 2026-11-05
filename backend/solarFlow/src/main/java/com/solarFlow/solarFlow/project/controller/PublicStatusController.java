package com.solarFlow.solarFlow.project.controller;

import com.solarFlow.solarFlow.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
public class PublicStatusController {

    private final ProjectService service;

    @GetMapping("/{token}")
    public ResponseEntity<PublicProjectStatusDTO> status(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.publicStatus(token));
    }
}