package com.solarFlow.solarFlow.pincode.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PincodeRequestDTO(

        @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "must be a 6-digit pincode")
        String pincode,

        @Size(max = 100)
        String areaName
) {
}