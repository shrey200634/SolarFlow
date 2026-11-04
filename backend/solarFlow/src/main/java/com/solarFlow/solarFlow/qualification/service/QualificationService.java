package com.solarFlow.solarFlow.qualification.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.bill.domain.BillExtraction;
import com.solarFlow.solarFlow.bill.repo.BillExtractionRepo;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.repo.LeadRepo;
import com.solarFlow.solarFlow.lead.service.LeadService;
import com.solarFlow.solarFlow.pincode.service.PincodeService;
import com.solarFlow.solarFlow.qualification.controller.QualificationResultDTO;
import com.solarFlow.solarFlow.qualification.domain.NextAction;
import com.solarFlow.solarFlow.qualification.domain.QualificationResult;
import com.solarFlow.solarFlow.qualification.dto.QualificationGate;
import com.solarFlow.solarFlow.qualification.dto.QualificationInput;
import com.solarFlow.solarFlow.qualification.dto.QualificationOutcome;
import com.solarFlow.solarFlow.qualification.dto.QualificationReason;
import com.solarFlow.solarFlow.qualification.repo.QualificationResultRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualificationService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LeadRepo leads;
    private final LeadService leadService;
    private final BillExtractionRepo extractions;
    private final QualificationResultRepo results;
    private final QualificationProperties properties;
    private final PincodeService pincodes;
    private final QualificationScorer scorer;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void qualifyAfterConfirmation(UUID leadId) {
        Lead lead = leads.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        evaluateAndSave(lead);
    }

    /** SALES / ADMIN re-run. Appends a new result row. */
    @Transactional
    public QualificationResultDTO runManually(UUID leadId, AuthUser actor) {
        Lead lead = leadService.findAccessible(leadId, actor);
        return toDto(evaluateAndSave(lead));
    }

    /** Latest result. Homeowners only get their own leads (404 otherwise). */
    @Transactional(readOnly = true)
    public QualificationResultDTO getLatest(UUID leadId, AuthUser actor) {
        leadService.findAccessible(leadId, actor);
        QualificationResult latest = results.findFirstByLeadIdOrderByCreatedAtDesc(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This lead has not been qualified yet"));
        return toDto(latest);
    }

    // ---------------- core ----------------

    private QualificationResult evaluateAndSave(Lead lead) {
        BillExtraction confirmed = extractions
                .findFirstByDocument_Lead_IdAndConfirmedAtIsNotNullOrderByConfirmedAtDesc(lead.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bill details have not been confirmed yet"));

        BigDecimal monthlyUnits = readMonthlyUnits(confirmed);
        Double billConfidence = readBillConfidence(confirmed);

        QualificationInput input = new QualificationInput(
                lead.getOwnership(),
                lead.getRoofType(),
                lead.getRoofAreaSqft(),
                pincodes.isServiceable(lead.getPincode()),
                monthlyUnits,
                billConfidence);

        QualificationOutcome outcome = scorer.score(input);

        QualificationResult saved = results.save(QualificationResult.builder()
                .lead(lead)
                .score(outcome.score())
                .nextAction(outcome.nextAction())
                .ruleVersion(properties.getRuleVersion())
                .reasonsJson(reasonsJson(outcome))
                .documentId(confirmed.getDocument().getId())
                .build());


        if (lead.getStatus() == LeadStatus.BILL_CONFIRMED) {
            lead.setStatus(outcome.nextAction() == NextAction.SURVEY_RECOMMENDED
                    ? LeadStatus.SURVEY_RECOMMENDED
                    : LeadStatus.SALES_CALL_PENDING);
        }

        log.info("Lead {} qualified: score={} action={} rules={}",
                lead.getId(), outcome.score(), outcome.nextAction(), properties.getRuleVersion());
        return saved;
    }

    // ---------------- reading the confirmed bill ----------------

    private static BigDecimal readMonthlyUnits(BillExtraction e) {
        Map<String, Object> confirmed = readMap(e.getConfirmedJson());
        Object field = confirmed.get("monthlyUnits");
        if (field instanceof Map<?, ?> m && m.get("value") != null) {
            try {
                return new BigDecimal(m.get("value").toString());
            } catch (NumberFormatException ex) {
                // fall through to the error below
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed bill has no valid monthly units");
    }

    /** The AI's overall confidence, or null when the bill was entered manually. */
    private static Double readBillConfidence(BillExtraction e) {
        if (e.getConfidenceJson() == null || e.getConfidenceJson().isBlank()) return null;
        Object overall = readMap(e.getConfidenceJson()).get("overall");
        return overall instanceof Number n ? n.doubleValue() : null;
    }

    // ---------------- JSON <-> DTO ----------------

    private static String reasonsJson(QualificationOutcome outcome) {
        List<Map<String, Object>> rules = new ArrayList<>();
        for (QualificationReason r : outcome.reasons()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r.code());
            m.put("points", r.points());
            m.put("maxPoints", r.maxPoints());
            m.put("passed", r.passed());
            m.put("text", r.text());
            rules.add(m);
        }
        List<Map<String, Object>> gates = new ArrayList<>();
        for (QualificationGate g : outcome.gates()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", g.code());
            m.put("triggered", g.triggered());
            m.put("text", g.text());
            gates.add(m);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("rules", rules);
        root.put("gates", gates);
        return JSON.writeValueAsString(root);
    }

    @SuppressWarnings("unchecked")
    private static QualificationResultDTO toDto(QualificationResult r) {
        Map<String, Object> details = readMap(r.getReasonsJson());
        List<Map<String, Object>> rules = (List<Map<String, Object>>) details.getOrDefault("rules", List.of());
        List<Map<String, Object>> gates = (List<Map<String, Object>>) details.getOrDefault("gates", List.of());
        return new QualificationResultDTO(r.getId(), r.getLead().getId(), r.getScore(), r.getNextAction(),
                r.getRuleVersion(), rules, gates, r.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, Object> result = JSON.readValue(json, Map.class);
            return result == null ? Map.of() : result;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not read stored JSON", e);
        }
    }
}