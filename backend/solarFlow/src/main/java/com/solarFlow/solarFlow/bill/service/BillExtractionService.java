package com.solarFlow.solarFlow.bill.service;

import com.solarFlow.solarFlow.bill.domain.BillExtraction;
import com.solarFlow.solarFlow.bill.domain.DocumentStatus;
import com.solarFlow.solarFlow.bill.domain.LeadDocument;
import com.solarFlow.solarFlow.bill.extraction.BillExtractor;
import com.solarFlow.solarFlow.bill.extraction.ExtractionException;
import com.solarFlow.solarFlow.bill.extraction.ExtractionResult;
import com.solarFlow.solarFlow.bill.extraction.ExtractionValidator;
import com.solarFlow.solarFlow.bill.repo.BillExtractionRepo;
import com.solarFlow.solarFlow.bill.repo.LeadDocumentRepo;
import com.solarFlow.solarFlow.bill.storage.DocumentStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Deliberately NOT @Transactional: the AI call can take 30s+ and must not hold a DB connection.
 * Each DB step runs in its own short transaction through TransactionTemplate.
 */
@Slf4j
@Service
public class BillExtractionService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LeadDocumentRepo documents;
    private final BillExtractionRepo extractions;
    private final DocumentStorage storage;
    private final BillExtractor extractor;
    private final ExtractionValidator validator;
    private final TransactionTemplate tx;
    private final int maxRetries;
    private final long backoffMs;

    public BillExtractionService(LeadDocumentRepo documents,
                                 BillExtractionRepo extractions,
                                 DocumentStorage storage,
                                 BillExtractor extractor,
                                 ExtractionValidator validator,
                                 PlatformTransactionManager txManager,
                                 @Value("${solarflow.bill.max-retries:2}") int maxRetries,
                                 @Value("${solarflow.bill.retry-backoff-ms:1000}") long backoffMs) {
        this.documents = documents;
        this.extractions = extractions;
        this.storage = storage;
        this.extractor = extractor;
        this.validator = validator;
        this.tx = new TransactionTemplate(txManager);
        this.maxRetries = maxRetries;
        this.backoffMs = backoffMs;
    }

    private record Claim(UUID documentId, String storageKey, String contentType,
                         BigDecimal declaredBill, int attempt) { }

    /** Idempotent: a document that is not in UPLOADED status is skipped. */
    public void extract(UUID documentId) {
        Optional<Claim> claimed = claim(documentId);
        if (claimed.isEmpty()) {
            log.info("Extraction skipped for document {}: not in UPLOADED status", documentId);
            return;
        }
        Claim c = claimed.get();

        try {
            byte[] data = readBytes(c.storageKey());
            ExtractionResult result = callWithRetry(data, c.contentType());
            ExtractionValidator.Report report = validator.validate(result, c.declaredBill());
            DocumentStatus status = saveSuccess(c, result, report);
            log.info("Extraction finished for document {}: status={} provider={} reviewFields={}",
                    documentId, status, result.provider(), report.reviewFields());
        } catch (ExtractionException e) {
            log.warn("Extraction failed for document {}: {} ({})", documentId, e.getCode(), e.getMessage());
            saveFailure(c, e.getCode());
        } catch (RuntimeException e) {
            log.error("Unexpected error extracting document {}", documentId, e);
            saveFailure(c, "INTERNAL_ERROR");
        }
    }

    // ---------------- step 1: claim ----------------

    private Optional<Claim> claim(UUID documentId) {
        Optional<Claim> result = tx.execute(status -> {
            int updated = documents.transition(documentId, DocumentStatus.UPLOADED, DocumentStatus.EXTRACTING);
            if (updated == 0) return Optional.<Claim>empty();

            LeadDocument d = documents.findById(documentId).orElseThrow();
            int attempt = extractions.findFirstByDocumentIdOrderByAttemptDesc(documentId)
                    .map(e -> e.getAttempt() + 1)
                    .orElse(1);
            return Optional.of(new Claim(d.getId(), d.getStorageKey(), d.getContentType(),
                    d.getLead().getDeclaredMonthlyBill(), attempt));
        });
        return result == null ? Optional.empty() : result;
    }

    // ---------------- steps 2 and 3: read file, call AI ----------------

    private byte[] readBytes(String storageKey) {
        try (InputStream in = storage.open(storageKey)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new ExtractionException("STORAGE_ERROR", false, "Could not read the stored bill");
        }
    }

    /** Blueprint: 2 retries, exponential backoff, transient errors only. */
    private ExtractionResult callWithRetry(byte[] data, String contentType) {
        for (int retry = 0; ; retry++) {
            try {
                return extractor.extract(data, contentType);
            } catch (ExtractionException e) {
                if (!e.isRetryable() || retry >= maxRetries) throw e;
                log.warn("Transient AI error {}; retry {} of {}", e.getCode(), retry + 1, maxRetries);
                backoff(retry);
            }
        }
    }

    private void backoff(int retry) {
        try {
            Thread.sleep(backoffMs * (1L << retry));      // 1s, 2s, 4s ...
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ExtractionException("INTERRUPTED", false, "Extraction was interrupted");
        }
    }

    // ---------------- step 4: save ----------------

    private DocumentStatus saveSuccess(Claim c, ExtractionResult result, ExtractionValidator.Report report) {
        DocumentStatus finalStatus = report.lowQuality() ? DocumentStatus.LOW_QUALITY : DocumentStatus.EXTRACTED;

        tx.executeWithoutResult(status -> {
            LeadDocument d = documents.findById(c.documentId()).orElseThrow();
            extractions.save(BillExtraction.builder()
                    .document(d)
                    .provider(limit(result.provider(), 30))
                    .extractedJson(result.rawJson())              // raw AI output, never edited afterwards
                    .confidenceJson(confidenceJson(result, report))
                    .attempt(c.attempt())
                    .build());
            d.setStatus(finalStatus);
        });
        return finalStatus;
    }

    private void saveFailure(Claim c, String errorCode) {
        try {
            tx.executeWithoutResult(status -> {
                LeadDocument d = documents.findById(c.documentId()).orElseThrow();
                extractions.save(BillExtraction.builder()
                        .document(d)
                        .provider(limit(extractor.name(), 30))
                        .attempt(c.attempt())
                        .errorCode(limit(errorCode, 40))
                        .build());
                d.setStatus(DocumentStatus.FAILED);               // customer goes to the manual-entry path
            });
        } catch (RuntimeException e) {
            log.error("Could not record extraction failure for document {}", c.documentId(), e);
        }
    }

    private static String confidenceJson(ExtractionResult r, ExtractionValidator.Report report) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (ExtractionValidator.FieldReport f : report.fields()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("confidence", f.confidence());
            m.put("reviewRequired", f.reviewRequired());
            m.put("issues", f.issues());
            fields.put(f.field(), m);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("fields", fields);
        root.put("overall", r.overallConfidence());
        root.put("imageQuality", r.imageQuality());
        root.put("lowQuality", report.lowQuality());
        return JSON.writeValueAsString(root);
    }

    private static String limit(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}