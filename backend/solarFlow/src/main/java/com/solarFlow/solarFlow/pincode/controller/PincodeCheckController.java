package com.solarFlow.solarFlow.pincode.controller;

import com.solarFlow.solarFlow.pincode.service.PincodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Any signed-in user can check whether we install in a pincode (the admin list stays admin-only).
@RestController
@RequestMapping("/api/v1/pincodes")
@RequiredArgsConstructor
public class PincodeCheckController {

    public record PincodeAvailabilityDTO(String pincode, boolean serviceable) { }

    private final PincodeService service;

    @GetMapping("/{pincode}/serviceable")
    public PincodeAvailabilityDTO check(@PathVariable String pincode) {
        if (!pincode.matches("^[1-9][0-9]{5}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "must be a 6-digit pincode");
        }
        return new PincodeAvailabilityDTO(pincode, service.isServiceable(pincode));
    }
}