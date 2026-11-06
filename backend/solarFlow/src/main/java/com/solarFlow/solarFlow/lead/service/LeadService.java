package com.solarFlow.solarFlow.lead.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.common.PageResponse;
import com.solarFlow.solarFlow.lead.controller.CreateLeadRequestDTO;
import com.solarFlow.solarFlow.lead.controller.LeadResponseDTO;
import com.solarFlow.solarFlow.lead.controller.LeadStatsDTO;
import com.solarFlow.solarFlow.lead.controller.UpdateLeadRequestDTO;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.repo.LeadRepo;
import com.solarFlow.solarFlow.qualification.domain.QualificationResult;
import com.solarFlow.solarFlow.qualification.repo.QualificationResultRepo;
import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.domain.User;
import com.solarFlow.solarFlow.user.repo.UserRepo;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepo leads;
    private final UserRepo users;
    private final QualificationResultRepo qualificationResults;

    @Transactional
    public LeadResponseDTO createLead(CreateLeadRequestDTO req, AuthUser actor) {
        String phoneE164 = normalisePhone(req.phone());
        boolean duplicate = leads.existsByPhoneE164(phoneE164);

        Lead lead = Lead.builder()
                .owner(users.getReferenceById(actor.id()))
                .ownership(req.ownership())
                .roofType(req.roofType())
                .roofAreaSqft(req.roofAreaSqft())
                .declaredMonthlyBill(req.declaredMonthlyBill())
                .pincode(req.pincode())
                .phoneE164(phoneE164)
                .duplicateFlag(duplicate)
                .build();

        return LeadResponseDTO.from(leads.save(lead), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponseDTO> list(AuthUser actor, LeadStatus status, String pincode,
                                              UUID assignee, String q, int page, int size) {
        boolean homeowner = actor.role() == Role.HOMEOWNER;

        Specification<Lead> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (homeowner) {
                p.add(cb.equal(root.get("owner").get("id"), actor.id()));   // data isolation
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (pincode != null && !pincode.isBlank()) {
                p.add(cb.equal(root.get("pincode"), pincode.trim()));
            }
            if (assignee != null && !homeowner) {
                p.add(cb.equal(root.get("assignedTo").get("id"), assignee));
            }
            // Staff search: customer name, email, or phone digits.
            if (!homeowner && q != null && !q.isBlank()) {
                String term = q.trim();
                String digits = term.replaceAll("\\D", "");
                String like = "%" + escapeLike(term.toLowerCase()) + "%";
                Join<Lead, User> owner = root.join("owner");
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(owner.get("fullName")), like, '\\'));
                any.add(cb.like(cb.lower(owner.get("email")), like, '\\'));
                if (digits.length() >= 3) {
                    any.add(cb.like(root.get("phoneE164"), "%" + digits + "%"));
                }
                p.add(cb.or(any.toArray(new Predicate[0])));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),            // cap page size at 100
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Lead> found = leads.findAll(spec, pageable);
        Map<UUID, Integer> scores = latestScores(found.getContent());
        return PageResponse.of(found.map(l -> LeadResponseDTO.from(l, scores.get(l.getId()))));
    }

    @Transactional(readOnly = true)
    public LeadResponseDTO get(UUID id, AuthUser actor) {
        Lead lead = findAccessible(id, actor);
        return LeadResponseDTO.from(lead, latestScore(lead.getId()));
    }

    @Transactional
    public LeadResponseDTO update(UUID id, UpdateLeadRequestDTO req) {
        Lead lead = leads.findById(id).orElseThrow(LeadService::notFound);

        if (req.assignedTo() != null) {
            User assignee = users.findById(req.assignedTo())
                    .filter(User::isActive)
                    .filter(u -> u.getRole() != Role.HOMEOWNER)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "assignee must be an active SALES or ADMIN user"));
            lead.setAssignedTo(assignee);
        }
        if (req.status() != null && req.status() != lead.getStatus()) {
            applyManualStatus(lead, req.status());
        }
        if (req.ownership() != null) {
            lead.setOwnership(req.ownership());
        }
        if (req.roofType() != null) {
            lead.setRoofType(req.roofType());
        }
        if (req.roofAreaSqft() != null) {
            lead.setRoofAreaSqft(req.roofAreaSqft());
        }
        return LeadResponseDTO.from(lead, latestScore(lead.getId()));
    }

    @Transactional(readOnly = true)
    public LeadStatsDTO stats() {
        Map<LeadStatus, Long> byStatus = new EnumMap<>(LeadStatus.class);
        for (LeadStatus s : LeadStatus.values()) {
            byStatus.put(s, 0L);
        }
        long total = 0;
        for (Object[] row : leads.countGroupedByStatus()) {
            long count = ((Number) row[1]).longValue();
            byStatus.put((LeadStatus) row[0], count);
            total += count;
        }
        return new LeadStatsDTO(total, byStatus);
    }

    // Reusable ownership guard. Homeowners get 404 for leads that are not theirs.
    @Transactional(readOnly = true)
    public Lead findAccessible(UUID id, AuthUser actor) {
        Lead lead = leads.findById(id).orElseThrow(LeadService::notFound);
        if (actor.role() == Role.HOMEOWNER && !lead.getOwner().getId().equals(actor.id())) {
            throw notFound();
        }
        return lead;
    }

    // ---------------- helpers ----------------

    /**
     * Pipeline statuses are set by the workflow itself (upload, confirm, qualify, quote, accept).
     * Staff may only mark an open lead as LOST, or reopen a lost lead for a sales call.
     */
    private static void applyManualStatus(Lead lead, LeadStatus target) {
        LeadStatus current = lead.getStatus();
        boolean markLost = target == LeadStatus.LOST && current != LeadStatus.ACCEPTED;
        boolean reopen = current == LeadStatus.LOST && target == LeadStatus.SALES_CALL_PENDING;
        if (!markLost && !reopen) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lead status cannot be changed from " + current + " to " + target + " manually");
        }
        lead.setStatus(target);
    }

    private Integer latestScore(UUID leadId) {
        return qualificationResults.findFirstByLeadIdOrderByCreatedAtDesc(leadId)
                .map(QualificationResult::getScore)
                .orElse(null);
    }

    private Map<UUID, Integer> latestScores(List<Lead> page) {
        if (page.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> ids = page.stream().map(Lead::getId).toList();
        return qualificationResults.findLatestForLeads(ids).stream()
                .collect(Collectors.toMap(
                        r -> r.getLead().getId(),
                        QualificationResult::getScore,
                        (a, b) -> a));
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found");
    }

    static String normalisePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        return "+91" + digits;
    }
}