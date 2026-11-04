package com.solarFlow.solarFlow.solar.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.domain.BillExtraction;
import com.solarFlow.solarFlow.bill.repo.BillExtractionRepo;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.service.LeadService;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;
import com.solarFlow.solarFlow.solar.dto.SolarSizingCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolarSizingService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LeadService leadService;
    private final BillExtractionRepo extractions;
    private final SolarSizingCalculator calculator;
    private final SolarSizingProperties properties;

    @Transactional(readOnly = true)
    public SolarRecommendationDTO recommend(UUID leadId, AuthUser actor) {

        // 1. Load lead and enforce homeowner ownership.
        Lead lead = leadService.findAccessible(leadId, actor);

        // 2. Only use the latest human-confirmed bill.
        BillExtraction extraction = extractions
                .findFirstByDocument_Lead_IdAndConfirmedAtIsNotNullOrderByConfirmedAtDesc(leadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Bill details have not been confirmed yet"
                ));

        // 3. Read confirmed values.
        Map<String, Object> confirmed = readJson(extraction.getConfirmedJson());

        BigDecimal monthlyUnits = readDecimalField(confirmed, "monthlyUnits", true);
        BigDecimal sanctionedLoadKw = readDecimalField(
                confirmed, "sanctionedLoadKw", false
        );

        // 4. Calculate deterministic system sizing.
        SolarSizingCalculation calculation = calculator.calculate(
                monthlyUnits,
                lead.getRoofAreaSqft(),
                sanctionedLoadKw
        );

        // 5. Build options.
        List<SolarRecommendationDTO.SolarOptionDTO> options = new ArrayList<>();

        if (calculation.economyCapacityKw().compareTo(BigDecimal.ZERO) > 0) {
            options.add(createOption("ECONOMY", calculation.economyCapacityKw()));
        }

        if (calculation.recommendedCapacityKw().compareTo(BigDecimal.ZERO) > 0) {
            options.add(createOption(
                    "RECOMMENDED",
                    calculation.recommendedCapacityKw()
            ));
        }

        if (calculation.maximumCapacityKw().compareTo(BigDecimal.ZERO) > 0) {
            options.add(createOption("MAXIMUM", calculation.maximumCapacityKw()));
        }

        // Remove duplicate capacities while retaining the first option label.
        options = options.stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                SolarRecommendationDTO.SolarOptionDTO::capacityKw,
                                option -> option,
                                (first, second) -> first,
                                java.util.LinkedHashMap::new
                        )
                )
                .values()
                .stream()
                .toList();

        String message = calculation.viable()
                ? "Solar capacity recommendations calculated successfully."
                : "The calculated capacity is below 1 kW. A sales representative should review this lead.";

        return new SolarRecommendationDTO(
                monthlyUnits,
                lead.getRoofAreaSqft(),
                sanctionedLoadKw,
                calculation.usageLimitKw(),
                calculation.roofLimitKw(),
                calculation.recommendedCapacityKw(),
                calculation.limitingFactor(),
                calculation.viable(),
                message,
                options
        );
    }

    private SolarRecommendationDTO.SolarOptionDTO createOption(
            String type,
            BigDecimal capacityKw
    ) {

        BigDecimal yearlyGeneration = capacityKw.multiply(
                properties.getAnnualGenerationPerKw()
        ).setScale(2, RoundingMode.HALF_UP);

        return new SolarRecommendationDTO.SolarOptionDTO(
                type,
                capacityKw,
                yearlyGeneration
        );
    }

    private static BigDecimal readDecimalField(
            Map<String, Object> confirmed,
            String fieldName,
            boolean required
    ) {

        Object field = confirmed.get(fieldName);

        if (field instanceof Map<?, ?> fieldMap) {
            Object value = fieldMap.get("value");

            if (value != null) {
                try {
                    return new BigDecimal(value.toString());
                } catch (NumberFormatException ignored) {
                    // Handled below.
                }
            }
        }

        if (!required) {
            return null;
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Confirmed bill has no valid " + fieldName
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirmed bill data is missing"
            );
        }

        try {
            Map<String, Object> result = JSON.readValue(json, Map.class);

            if (result == null) {
                throw new IllegalStateException("Empty JSON");
            }

            return result;

        } catch (RuntimeException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read confirmed bill data"
            );
        }
    }
}