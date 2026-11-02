package com.solarFlow.solarFlow.lead;

import com.solarFlow.solarFlow.auth.AuthUser;
import com.solarFlow.solarFlow.user.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepo leads;
    private final UserRepo users;

    @Transactional
    public LeadResponseDTO createLead(CreateLeadRequestDTO req, AuthUser actor) {
        String phoneE164 = normalisePhone(req.phone());

        // flag duplicates, never auto-merge
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

       static String normalisePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        return "+91" + digits;
    }
}



