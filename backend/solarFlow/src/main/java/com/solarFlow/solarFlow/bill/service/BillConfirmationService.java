package com.solarFlow.solarFlow.bill.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.controller.ConfirmBillRequestDTO;
import com.solarFlow.solarFlow.bill.controller.ExtractionResponseDTO;
import com.solarFlow.solarFlow.bill.domain.BillExtraction;
import com.solarFlow.solarFlow.bill.domain.DocumentStatus;
import com.solarFlow.solarFlow.bill.domain.LeadDocument;
import com.solarFlow.solarFlow.bill.event.BillConfirmedEvent;
import com.solarFlow.solarFlow.bill.repo.BillExtractionRepo;
import com.solarFlow.solarFlow.bill.repo.LeadDocumentRepo;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.service.LeadService;
import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillConfirmationService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LeadDocumentRepo documents;
    private final BillExtractionRepo extractions;
    private final LeadService leadService;
    private final UserRepo users;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public ExtractionResponseDTO getExtraction(
            UUID documentId,
            AuthUser actor
    ) {

        // Homeowners can only access documents belonging to their own leads.
        LeadDocument document = findAccessibleDocument(documentId, actor);

        BillExtraction extraction = extractions
                .findFirstByDocumentIdOrderByAttemptDesc(documentId)
                .orElse(null);

        Map<String, Object> extractedFields = Map.of();
        Map<String, Object> confidence = Map.of();
        Map<String, Object> confirmedFields = Map.of();

        String provider = null;
        String errorCode = null;

        if (extraction != null) {

            extractedFields = readJson(extraction.getExtractedJson());
            confidence = readJson(extraction.getConfidenceJson());
            confirmedFields = readJson(extraction.getConfirmedJson());

            provider = extraction.getProvider();
            errorCode = extraction.getErrorCode();
        }

        return new ExtractionResponseDTO(
                document.getId(),
                document.getStatus(),
                provider,
                extractedFields,
                confidence,
                confirmedFields,
                errorCode
        );
    }

    @Transactional
    public ExtractionResponseDTO confirm(
            UUID documentId,
            ConfirmBillRequestDTO request,
            AuthUser actor
    ) {

        // Ownership first: a foreign or unknown document gets the same 404 and nothing is changed.
        findAccessibleDocument(documentId, actor);

        // Only these states can enter the confirmation flow.
        int updated = documents.transitionFrom(
                documentId,
                List.of(
                        DocumentStatus.EXTRACTED,
                        DocumentStatus.LOW_QUALITY,
                        DocumentStatus.FAILED
                ),
                DocumentStatus.CONFIRMED
        );

        if (updated == 0) {
            if (!documents.existsById(documentId)) {
                throw notFound();
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be confirmed in its current status"
            );
        }

        LeadDocument document = documents.findById(documentId)
                .orElseThrow(BillConfirmationService::notFound);

        Lead lead = document.getLead();

        BillExtraction extraction = extractions
                .findFirstByDocumentIdOrderByAttemptDesc(documentId)
                .orElse(null);

        Map<String, Object> aiFields = extraction == null
                ? Map.of()
                : readJson(extraction.getExtractedJson());

        Map<String, Object> confirmed = new LinkedHashMap<>();

        // Merge AI values with customer/sales corrections.
        addConfirmedField(
                confirmed, aiFields, "consumerNumber",
                request.consumerNumber(), actor
        );

        addConfirmedField(
                confirmed, aiFields, "tariff",
                request.tariff(), actor
        );

        addConfirmedField(
                confirmed, aiFields, "sanctionedLoadKw",
                request.sanctionedLoadKw(), actor
        );

        addConfirmedField(
                confirmed, aiFields, "monthlyUnits",
                request.monthlyUnits(), actor
        );

        // Do not confirm a bill if any required value is still missing.
        validateConfirmedFields(confirmed);

        Instant confirmedAt = Instant.now();

        String confirmedJson = JSON.writeValueAsString(confirmed);

        if (extraction == null) {
            extraction = BillExtraction.builder()
                    .document(document)
                    .provider("manual")
                    .attempt(1)
                    .build();
        }

        extraction.setConfirmedJson(confirmedJson);
        extraction.setConfirmedBy(users.getReferenceById(actor.id()));
        extraction.setConfirmedAt(confirmedAt);

        extractions.save(extraction);

        // Do not regress a lead that has already progressed further.
        if (lead.getStatus() == LeadStatus.NEW
                || lead.getStatus() == LeadStatus.BILL_UPLOADED) {
            lead.setStatus(LeadStatus.BILL_CONFIRMED);
        }

        events.publishEvent(new BillConfirmedEvent(
                lead.getId(),
                document.getId(),
                actor.id(),
                confirmedAt
        ));

        return getExtraction(documentId, actor);
    }

    private static void addConfirmedField(
            Map<String, Object> confirmed,
            Map<String, Object> aiFields,
            String fieldName,
            Object customerValue,
            AuthUser actor
    ) {

        Object aiValue = extractValue(aiFields.get(fieldName));

        Object finalValue = customerValue != null
                ? customerValue
                : aiValue;

        String source;

        if (customerValue != null) {
            source = actor.role() == Role.SALES
                    ? "SALES"
                    : "CUSTOMER";
        } else if (aiValue != null) {
            source = "AI";
        } else {
            source = "CUSTOMER";
        }

        Map<String, Object> field = new LinkedHashMap<>();
        field.put("value", finalValue);
        field.put("source", source);

        confirmed.put(fieldName, field);
    }

    private static Object extractValue(Object field) {

        if (!(field instanceof Map<?, ?> map)) {
            return null;
        }

        return map.get("value");
    }

    private static void validateConfirmedFields(
            Map<String, Object> confirmed
    ) {

        for (String fieldName : List.of(
                "consumerNumber",
                "tariff",
                "sanctionedLoadKw",
                "monthlyUnits"
        )) {

            Object field = confirmed.get(fieldName);

            if (!(field instanceof Map<?, ?> map)
                    || map.get("value") == null
                    || map.get("value").toString().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing required bill field: " + fieldName
                );
            }
        }

        Object consumerNumber = extractValue(
                confirmed.get("consumerNumber")
        );

        Object tariff = extractValue(
                confirmed.get("tariff")
        );

        if (consumerNumber.toString().length() > 50
                || tariff.toString().length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bill field exceeds allowed length"
            );
        }

        Object load = extractValue(confirmed.get("sanctionedLoadKw"));
        Object units = extractValue(confirmed.get("monthlyUnits"));

        try {
            BigDecimal loadValue = new BigDecimal(load.toString());
            BigDecimal unitsValue = new BigDecimal(units.toString());

            if (loadValue.compareTo(BigDecimal.ZERO) <= 0
                    || unitsValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sanctioned load or monthly units"
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Object> result = JSON.readValue(json, Map.class);
            return result == null ? Map.of() : result;
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Could not read stored extraction JSON", e
            );
        }
    }

    /**
     * Loads a document only if the actor may see its lead. A missing document and a document
     * that belongs to someone else both produce the identical "Document not found" 404,
     * so the API never reveals which document ids exist.
     */
    private LeadDocument findAccessibleDocument(UUID documentId, AuthUser actor) {

        LeadDocument document = documents.findById(documentId)
                .orElseThrow(BillConfirmationService::notFound);

        try {
            leadService.findAccessible(document.getLead().getId(), actor);
        } catch (ResponseStatusException e) {
            throw notFound();
        }

        return document;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Document not found"
        );
    }
}