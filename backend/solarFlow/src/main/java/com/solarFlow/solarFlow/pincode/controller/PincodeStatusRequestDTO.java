package com.solarFlow.solarFlow.pincode.controller;

import jakarta.validation.constraints.NotNull;

public record PincodeStatusRequestDTO(

        @NotNull
        Boolean active
) {
}