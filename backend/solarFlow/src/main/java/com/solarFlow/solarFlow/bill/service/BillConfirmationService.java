
package com.solarFlow.solarFlow.bill.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.controller.ConfirmBillRequestDTO;
import com.solarFlow.solarFlow.bill.controller.ExtractionResponseDTO;
import com.solarFlow.solarFlow.bill.domain.BillExtraction;
import com.solarFlow.solarFlow.bill.domain.DocumentStatus;
import com.solarFlow.solarFlow.bill.domain.LeadDocument;
import com.solarFlow.solarFlow.bill.event.BillConfirmedEvent;
import com.solarFlow.solarFlow.bill.extraction.ExtractionValidator;
import com.solarFlow.solarFlow.bill.repo.BillExtractionRepo;
import com.solarFlow.solarFlow.bill.repo.LeadDocumentRepo;
import com.solarFlow.solarFlow.bill.storage.DocumentStorage;
import com.solarFlow.solarFlow.bill.storage.StorageException;
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

import java.io.IOException;
import java.io.InputStream;
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
    private final ExtractionValidator validator;
    private final DocumentStorage storage;

    public record StoredFile(byte[] data, String contentType) { }

    @Transactional(readOnly = true)
    public StoredFile readFile(UUID documentId, AuthUser actor) {
        LeadDocument document = findAccessibleDocument(documentId, actor);
        try (InputStream in = storage.open(document.getStorageKey())) {
            return new StoredFile(in.readAllBytes(), document.getContentType());
        } catch (IOException | StorageException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill file is not available");
        }
    }

    @Transactional(readOnly = true)
    public ExtractionResponseDTO getExtraction(
            UUID documentId,
            AuthUser actor
    ) {
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
        // Ownership is checked before reading or modifying document data.
        LeadDocument document = findAccessibleDocument(documentId, actor);
        Lead lead = document.getLead();

        BillExtraction extraction = extractions
                .findFirstByDocumentIdOrderByAttemptDesc(documentId)
                .orElse(null);

        Map<String, Object> aiFields = extraction == null
                ? Map.of()
                : readJson(extraction.getExtractedJson());

        Map<String, Object> confirmed = new LinkedHashMap<>();

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

        // Validate the final merged values, including unchanged AI values.
        List<String> issues = validator.validateConfirmedValues(
                stringValue(confirmed, "consumerNumber"),
                stringValue(confirmed, "tariff"),
                decimalValue(confirmed, "sanctionedLoadKw"),
                decimalValue(confirmed, "monthlyUnits"),
                lead.getDeclaredMonthlyBill()
        );

        if (!issues.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bill confirmation validation failed: "
                            + String.join(", ", issues)
            );
        }

        // Only valid data may move the document into CONFIRMED.
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
            Object correctedValue,
            AuthUser actor
    ) {
        Object aiValue = extractValue(aiFields.get(fieldName));

        Object finalValue = correctedValue != null
                ? correctedValue
                : aiValue;

        String source;

        if (correctedValue != null) {
            source = actor.role() != Role.HOMEOWNER
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

    private static String stringValue(
            Map<String, Object> fields,
            String fieldName
    ) {
        Object value = extractValue(fields.get(fieldName));
        return value == null ? null : value.toString();
    }

    private static BigDecimal decimalValue(
            Map<String, Object> fields,
            String fieldName
    ) {
        Object value = extractValue(fields.get(fieldName));

        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
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
                    "Could not read stored extraction JSON",
                    e
            );
        }
    }

    /**
     * A foreign document and an unknown document both return the same 404.
     */
    private LeadDocument findAccessibleDocument(
            UUID documentId,
            AuthUser actor
    ) {
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
