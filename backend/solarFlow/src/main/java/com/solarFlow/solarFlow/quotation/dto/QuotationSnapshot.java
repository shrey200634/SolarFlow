package com.solarFlow.solarFlow.quotation.dto;

import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record QuotationSnapshot(

        UUID leadId,

        LocalDate quotationDate,

        String selectedOptionType,

        BigDecimal selectedCapacityKw,

        SolarRecommendationDTO sizing,

        BigDecimal annualGenerationPerKw,

        BigDecimal roofAreaPerKw,

        PricingCalculationDTO pricing,

        String subsidyDisclaimer,

        String pricingDisclaimer

) {
}