package com.solarFlow.solarFlow.bill.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConfirmBillRequestDTO(

        @Size(max = 50)
        String consumerNumber,

        @Size(max = 50)
        String tariff,

        @DecimalMin("0.01")
        BigDecimal sanctionedLoadKw,

        @DecimalMin("0.0")
        BigDecimal monthlyUnits

) {
}