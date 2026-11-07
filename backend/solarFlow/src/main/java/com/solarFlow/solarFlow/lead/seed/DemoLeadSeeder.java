package com.solarFlow.solarFlow.lead.seed;

import com.solarFlow.solarFlow.bill.domain.BillExtraction;
import com.solarFlow.solarFlow.bill.domain.DocumentStatus;
import com.solarFlow.solarFlow.bill.domain.LeadDocument;
import com.solarFlow.solarFlow.bill.repo.BillExtractionRepo;
import com.solarFlow.solarFlow.bill.repo.LeadDocumentRepo;
import com.solarFlow.solarFlow.bill.storage.DocumentStorage;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.domain.Ownership;
import com.solarFlow.solarFlow.lead.domain.RoofType;
import com.solarFlow.solarFlow.lead.repo.LeadRepo;
import com.solarFlow.solarFlow.qualification.service.QualificationService;
import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.domain.User;
import com.solarFlow.solarFlow.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class DemoLeadSeeder {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final double SEED_CONFIDENCE = 0.95;

    private record Sample(String ref, String name, String email, String phone,
                          Ownership ownership, RoofType roofType, String roofAreaSqft,
                          String monthlyBill, String pincode,
                          String consumerNumber, String sanctionedLoadKw, String monthlyUnits) { }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("LD-30881", "Prashant Wankhede", "prashant@solarflow.local", "9822030881",
                    Ownership.OWNED, RoofType.RCC_FLAT, "400", "3200", "440010",
                    "410012330881", "5", "340"),
            new Sample("LD-30902", "Rohit Meshram", "rohit@solarflow.local", "9822030902",
                    Ownership.RENTED, RoofType.RCC_FLAT, "400", "1900", "440010",
                    "410012330902", "3", "210"),
            new Sample("LD-30915", "Ayesha Siddiqui", "ayesha@solarflow.local", "9822030915",
                    Ownership.OWNED, RoofType.RCC_FLAT, "300", "650", "440010",
                    "410012330915", "2", "85")
    );

    private final UserRepo users;
    private final LeadRepo leads;
    private final LeadDocumentRepo documents;
    private final BillExtractionRepo extractions;
    private final DocumentStorage storage;
    private final PasswordEncoder passwordEncoder;
    private final QualificationService qualification;
    private final PlatformTransactionManager txManager;

    @Value("${app.seed.password:}")
    private String seedPassword;

    @Value("${app.seed.demo-leads:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!enabled) {
            return;
        }
        if (seedPassword == null || seedPassword.isBlank()) {
            log.info("Demo lead seeding skipped: SEED_PASSWORD is not configured.");
            return;
        }

        TransactionTemplate tx = new TransactionTemplate(txManager);
        for (Sample s : SAMPLES) {
            if (users.existsByEmail(s.email())) {
                continue;
            }
            try {
                UUID leadId = tx.execute(status -> createLead(s));
                // Same scoring as a real confirmation (runs in its own transaction, sets the lead status).
                qualification.qualifyAfterConfirmation(leadId);
                log.info("Seeded demo lead {} ({}).", s.ref(), s.name());
            } catch (RuntimeException e) {
                log.error("Could not seed demo lead {}", s.ref(), e);
            }
        }
    }

    private UUID createLead(Sample s) {
        User owner = users.save(User.builder()
                .email(s.email())
                .fullName(s.name())
                .phone(s.phone())
                .passwordHash(passwordEncoder.encode(seedPassword))
                .role(Role.HOMEOWNER)
                .active(true)
                .consentAt(Instant.now())
                .consentVersion("v1")
                .build());

        String phoneE164 = "+91" + s.phone();
        Lead lead = leads.save(Lead.builder()
                .owner(owner)
                .assignedTo(users.findByEmail("sales@solarflow.local").orElse(null))
                .ownership(s.ownership())
                .roofType(s.roofType())
                .roofAreaSqft(new BigDecimal(s.roofAreaSqft()))
                .declaredMonthlyBill(new BigDecimal(s.monthlyBill()))
                .pincode(s.pincode())
                .phoneE164(phoneE164)
                .duplicateFlag(leads.existsByPhoneE164(phoneE164))
                .status(LeadStatus.BILL_CONFIRMED)
                .build());

        // A clearly-labelled sample bill, stored privately like a real upload.
        byte[] pdf = sampleBillPdf(s);
        String key = storage.save(new ByteArrayInputStream(pdf));
        LeadDocument document = documents.save(LeadDocument.builder()
                .lead(lead)
                .storageKey(key)
                .contentType("application/pdf")
                .sizeBytes(pdf.length)
                .sha256(sha256(pdf))
                .status(DocumentStatus.CONFIRMED)
                .build());

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("consumerNumber", s.consumerNumber());
        values.put("tariff", "LT-I Residential");
        values.put("sanctionedLoadKw", new BigDecimal(s.sanctionedLoadKw()));
        values.put("monthlyUnits", new BigDecimal(s.monthlyUnits()));

        Map<String, Object> extracted = new LinkedHashMap<>();
        Map<String, Object> confidenceFields = new LinkedHashMap<>();
        Map<String, Object> confirmed = new LinkedHashMap<>();
        values.forEach((field, value) -> {
            extracted.put(field, Map.of("value", value, "confidence", SEED_CONFIDENCE));
            confidenceFields.put(field, Map.of(
                    "confidence", SEED_CONFIDENCE, "reviewRequired", false, "issues", List.of()));
            confirmed.put(field, Map.of("value", value, "source", "AI"));
        });

        Map<String, Object> confidence = new LinkedHashMap<>();
        confidence.put("fields", confidenceFields);
        confidence.put("overall", SEED_CONFIDENCE);
        confidence.put("imageQuality", "GOOD");
        confidence.put("lowQuality", false);

        extractions.save(BillExtraction.builder()
                .document(document)
                .provider("seed-sample")
                .attempt(1)
                .extractedJson(JSON.writeValueAsString(extracted))
                .confidenceJson(JSON.writeValueAsString(confidence))
                .confirmedJson(JSON.writeValueAsString(confirmed))
                .confirmedBy(owner)
                .confirmedAt(Instant.now())
                .build());

        return lead.getId();
    }

    private static byte[] sampleBillPdf(Sample s) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A5);
            doc.addPage(page);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<String[]> rows = List.of(
                    new String[]{"Consumer name", s.name()},
                    new String[]{"Consumer number", s.consumerNumber()},
                    new String[]{"Tariff", "LT-I Residential"},
                    new String[]{"Sanctioned load", s.sanctionedLoadKw() + " kW"},
                    new String[]{"Units consumed", s.monthlyUnits() + " kWh"},
                    new String[]{"Bill amount", "INR " + s.monthlyBill()});
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - 50;
                write(cs, bold, 14, 40, y, "SAMPLE ELECTRICITY BILL");
                write(cs, regular, 9, 40, y - 16, "Demo data for SolarFlow (" + s.ref() + "). Not a real bill.");
                y -= 50;
                for (String[] r : rows) {
                    write(cs, regular, 11, 40, y, r[0]);
                    write(cs, bold, 11, 190, y, r[1]);
                    y -= 22;
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build sample bill PDF", e);
        }
    }

    private static void write(PDPageContentStream cs, PDType1Font font, float size,
                              float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}