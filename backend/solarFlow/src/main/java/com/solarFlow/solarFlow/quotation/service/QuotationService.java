package com.solarFlow.solarFlow.quotation.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.repo.LeadRepo;
import com.solarFlow.solarFlow.lead.service.LeadService;
import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.pricing.service.PricingCalculationService;
import com.solarFlow.solarFlow.quotation.domain.Quotation;
import com.solarFlow.solarFlow.quotation.domain.QuotationStatus;
import com.solarFlow.solarFlow.quotation.dto.CreateQuotationRequestDTO;
import com.solarFlow.solarFlow.quotation.dto.QuotationResponseDTO;
import com.solarFlow.solarFlow.quotation.dto.QuotationSnapshot;
import com.solarFlow.solarFlow.quotation.repo.QuotationRepo;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;
import com.solarFlow.solarFlow.solar.service.SolarSizingProperties;
import com.solarFlow.solarFlow.solar.service.SolarSizingService;
import com.solarFlow.solarFlow.user.domain.Role;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepo quotationRepo;
    private final LeadRepo leadRepo;
    private final LeadService leadService;
    private final SolarSizingService solarSizingService;
    private final PricingCalculationService pricingCalculationService;
    private final SolarSizingProperties solarSizingProperties;
    private final JsonMapper jsonMapper;
    private final QuotationPdfService quotationPdfService;

    @Transactional
    public QuotationResponseDTO create(
            UUID leadId,
            CreateQuotationRequestDTO request,
            AuthUser actor
    ) {

        if (actor.role() != Role.SALES && actor.role() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only sales staff or admin can generate quotations"
            );
        }

        Lead lead = leadService.findAccessible(leadId, actor);

        SolarRecommendationDTO sizing =
                solarSizingService.recommend(leadId, actor);

        if (!sizing.viable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This lead is not currently viable for a standard solar system"
            );
        }

        BigDecimal selectedCapacity = request.selectedCapacityKw();

        SolarRecommendationDTO.SolarOptionDTO selectedOption =
                sizing.options().stream()
                        .filter(option ->
                                option.capacityKw().compareTo(selectedCapacity) == 0)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Selected capacity is not one of the recommended options"
                        ));

        PricingCalculationDTO pricing =
                pricingCalculationService.calculate(selectedCapacity);

        QuotationSnapshot snapshot = new QuotationSnapshot(
                leadId,
                LocalDate.now(),
                selectedOption.type(),
                selectedCapacity,
                sizing,
                solarSizingProperties.getAnnualGenerationPerKw(),
                solarSizingProperties.getRoofAreaPerKw(),
                pricing,
                pricing.subsidyEstimated()
                        ? "This subsidy is an estimate based on an unverified rule. "
                        + "Actual eligibility and amount must be confirmed."
                        : "Subsidy is based on the configured rule and must be confirmed.",
                "Pricing, electricity tariff, generation and payback are estimates "
                        + "based on the rules effective on the quotation date."
        );

        String snapshotJson;

        try {
            snapshotJson = jsonMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to save quotation snapshot", exception
            );
        }

        Quotation quotation = Quotation.builder()
                .lead(lead)
                .selectedCapacityKw(selectedCapacity)
                .selectedOptionType(selectedOption.type())
                .snapshotJson(snapshotJson)
                .status(QuotationStatus.ISSUED)
                .build();

        Quotation saved = quotationRepo.save(quotation);

        lead.setStatus(LeadStatus.QUOTED);
        leadRepo.save(lead);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuotationResponseDTO get(UUID quotationId, AuthUser actor) {

        Quotation quotation = getQuotationOrThrow(quotationId);

        // Enforces homeowner ownership. Staff can access any lead.
        leadService.findAccessible(quotation.getLead().getId(), actor);

        return toResponse(quotation);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(UUID quotationId, AuthUser actor) {

        Quotation quotation = getQuotationOrThrow(quotationId);

        leadService.findAccessible(quotation.getLead().getId(), actor);

        QuotationSnapshot snapshot = readSnapshot(quotation.getSnapshotJson());

        return quotationPdfService.generate(snapshot);
    }

    @Transactional
    public QuotationResponseDTO accept(UUID quotationId, AuthUser actor) {

        if (actor.role() != Role.HOMEOWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the homeowner can accept a quotation"
            );
        }

        Quotation quotation = getQuotationOrThrow(quotationId);

        Lead lead = leadService.findAccessible(
                quotation.getLead().getId(), actor
        );

        // Idempotent acceptance: repeated requests do not change the result.
        if (quotation.getStatus() == QuotationStatus.ACCEPTED) {
            return toResponse(quotation);
        }

        quotation.setStatus(QuotationStatus.ACCEPTED);
        quotation.setAcceptedAt(Instant.now());

        lead.setStatus(LeadStatus.ACCEPTED);

        leadRepo.save(lead);
        Quotation saved = quotationRepo.save(quotation);

        return toResponse(saved);
    }

    private Quotation getQuotationOrThrow(UUID quotationId) {

        return quotationRepo.findById(quotationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Quotation not found"
                ));
    }

    private QuotationSnapshot readSnapshot(String snapshotJson) {

        try {
            return jsonMapper.readValue(
                    snapshotJson,
                    QuotationSnapshot.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to read quotation snapshot", exception
            );
        }
    }

    private QuotationResponseDTO toResponse(Quotation quotation) {

        return new QuotationResponseDTO(
                quotation.getId(),
                quotation.getLead().getId(),
                quotation.getSelectedCapacityKw(),
                quotation.getSelectedOptionType(),
                quotation.getStatus(),
                quotation.getCreatedAt(),
                quotation.getAcceptedAt(),
                readSnapshot(quotation.getSnapshotJson())
        );
    }
}