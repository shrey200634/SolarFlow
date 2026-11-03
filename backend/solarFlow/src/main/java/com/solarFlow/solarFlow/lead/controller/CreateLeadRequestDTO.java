package com.solarFlow.solarFlow.lead.controller;

import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.lead.domain.RoofType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateLeadRequestDTO(

        @NotNull
        Ownership ownership,

        @NotNull
        RoofType roofType,

        @NotNull @DecimalMin(value = "50.0", message = "roof area must be at least 50 sq ft")
        @DecimalMax(value = "100000.0") @Digits(integer = 6, fraction = 1)
        BigDecimal roofAreaSqft,

        @NotNull @DecimalMin(value = "1.00", message = "monthly bill must be positive")
        @DecimalMax(value = "10000000.00") @Digits(integer = 10, fraction = 2)
        BigDecimal declaredMonthlyBill,

        @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "must be a 6-digit pincode")
        String pincode,

        @NotBlank
        @Pattern(regexp = "^(\\+?91[\\s-]?|0)?[6-9][0-9]{4}[\\s-]?[0-9]{5}$",
                message = "must be a valid Indian mobile number")
        String phone

) {
}