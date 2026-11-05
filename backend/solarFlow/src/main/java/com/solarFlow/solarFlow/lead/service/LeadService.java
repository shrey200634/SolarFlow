package com.solarFlow.solarFlow.lead.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.common.PageResponse;
import com.solarFlow.solarFlow.lead.controller.CreateLeadRequestDTO;
import com.solarFlow.solarFlow.lead.controller.LeadResponseDTO;
import com.solarFlow.solarFlow.lead.controller.UpdateLeadRequestDTO;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.repo.LeadRepo;
import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.domain.User;
import com.solarFlow.solarFlow.user.repo.UserRepo;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepo leads;
    private final UserRepo users;

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

        return LeadResponseDTO.from(leads.save(lead));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponseDTO> list(AuthUser actor, LeadStatus status, String pincode,
                                              UUID assignee, int page, int size) {
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
            return cb.and(p.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),            // cap page size at 100
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<LeadResponseDTO> result = leads.findAll(spec, pageable).map(LeadResponseDTO::from);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public LeadResponseDTO get(UUID id, AuthUser actor) {
        return LeadResponseDTO.from(findAccessible(id, actor));
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
        if (req.status() != null) {
            lead.setStatus(req.status());
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
        return LeadResponseDTO.from(lead);
    }


     // Reusable ownership guard. Homeowners get 404 for leads that are not theirs.
     //Bill upload (Step 4/5) will call this too.

    @Transactional(readOnly = true)
    public Lead findAccessible(UUID id, AuthUser actor) {
        Lead lead = leads.findById(id).orElseThrow(LeadService::notFound);
        if (actor.role() == Role.HOMEOWNER && !lead.getOwner().getId().equals(actor.id())) {
            throw notFound();
        }
        return lead;
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
