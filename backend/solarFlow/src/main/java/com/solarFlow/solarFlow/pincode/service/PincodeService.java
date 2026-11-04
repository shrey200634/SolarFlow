package com.solarFlow.solarFlow.pincode.service;

import com.solarFlow.solarFlow.pincode.controller.PincodeRequestDTO;
import com.solarFlow.solarFlow.pincode.controller.PincodeResponseDTO;
import com.solarFlow.solarFlow.pincode.domain.ServiceablePincode;
import com.solarFlow.solarFlow.pincode.repo.ServiceablePincodeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PincodeService {

    private final ServiceablePincodeRepo pincodes;

    @Transactional(readOnly = true)
    public boolean isServiceable(String pincode) {
        return pincode != null && pincodes.existsByPincodeAndActiveTrue(pincode.trim());
    }

    @Transactional(readOnly = true)
    public List<PincodeResponseDTO> list() {
        return pincodes.findAllByOrderByPincodeAsc().stream().map(PincodeResponseDTO::from).toList();
    }

    @Transactional
    public PincodeResponseDTO add(PincodeRequestDTO req) {
        String code = req.pincode().trim();

        if (pincodes.existsByPincode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pincode already exists");
        }

        ServiceablePincode saved = pincodes.save(ServiceablePincode.builder()
                .pincode(code)
                .areaName(req.areaName() == null || req.areaName().isBlank() ? null : req.areaName().trim())
                .active(true)
                .build());

        return PincodeResponseDTO.from(saved);
    }

    @Transactional
    public PincodeResponseDTO setActive(UUID id, boolean active) {
        ServiceablePincode p = pincodes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pincode not found"));
        p.setActive(active);
        return PincodeResponseDTO.from(p);
    }
}