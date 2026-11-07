package com.solarFlow.solarFlow.quotation.service;

import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.quotation.dto.QuotationSnapshot;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuotationPdfService {

    private static final float MARGIN = 50f;
    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;

    public byte[] generate(QuotationSnapshot s) {

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PricingCalculationDTO p = s.pricing();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float y = PAGE_H - MARGIN;

                // ---- title ----
                text(cs, bold, 18, MARGIN, y - 14, "SOLARFLOW - SOLAR QUOTATION");
                y -= 36;
                text(cs, regular, 9, MARGIN, y,
                        "Quotation date: " + s.quotationDate() + "   |   Lead reference: " + s.leadId());
                y -= 22;

                // ---- headline box (the story first) ----
                float boxH = 92f;
                cs.setNonStrokingColor(0.92f, 0.96f, 0.93f);
                cs.addRect(MARGIN, y - boxH, CONTENT_W, boxH);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);

                text(cs, bold, 16, MARGIN + 14, y - 28,
                        "Your " + plain(s.selectedCapacityKw()) + " kW rooftop solar system");
                text(cs, bold, 12, MARGIN + 14, y - 50,
                        "You pay " + inr(p.netSystemCost()) + " after estimated subsidy");
                String payback = p.estimatedPaybackYears() == null
                        ? "Payback period not available"
                        : "Pays for itself in about " + plain(p.estimatedPaybackYears()) + " years";
                text(cs, regular, 11, MARGIN + 14, y - 68,
                        payback + "  |  about " + units(p.annualGenerationUnits()) + " units generated per year");
                y -= boxH + 26;

                // ---- system ----
                y = section(cs, bold, y, "Selected system");
                y = row(cs, regular, y, "Option", pretty(s.selectedOptionType()), false);
                y = row(cs, regular, y, "System capacity", plain(s.selectedCapacityKw()) + " kW", false);
                y = row(cs, regular, y, "Estimated yearly generation",
                        units(p.annualGenerationUnits()) + " units", false);
                if (s.sizing() != null && s.sizing().limitingFactor() != null) {
                    y = row(cs, regular, y, "Recommended size limited by",
                            pretty(s.sizing().limitingFactor()), false);
                }
                y -= 12;

                // ---- all options from the sizing snapshot ----
                if (s.sizing() != null && s.sizing().options() != null && !s.sizing().options().isEmpty()) {
                    y = section(cs, bold, y, "System options");
                    for (SolarRecommendationDTO.SolarOptionDTO o : s.sizing().options()) {
                        boolean chosen = o.capacityKw().compareTo(s.selectedCapacityKw()) == 0;
                        y = row(cs, chosen ? bold : regular, y,
                                pretty(o.type()) + (chosen ? "  (this quotation)" : ""),
                                plain(o.capacityKw()) + " kW  |  " + units(o.estimatedYearlyGeneration()) + " units / year",
                                false);
                    }
                    text(cs, regular, 8.5f, MARGIN, y + 4,
                            "Ask us for a priced quotation for any other option.");
                    y -= 14;
                }

                // ---- cost ----
                y = section(cs, bold, y, "Cost breakdown");
                y = row(cs, regular, y, "Price per kW", inr(p.pricePerKw()), false);
                y = row(cs, regular, y, "Gross system cost", inr(p.grossSystemCost()), false);
                y = row(cs, regular, y,
                        p.subsidyEstimated() ? "Subsidy (estimate)" : "Subsidy",
                        "- " + inr(p.subsidyAmount()), false);
                y = row(cs, bold, y, "Net payable", inr(p.netSystemCost()), true);
                y -= 12;

                // ---- savings ----
                y = section(cs, bold, y, "Savings");
                y = row(cs, regular, y, "Electricity tariff assumed",
                        inr(p.electricityTariffPerUnit()) + " / unit", false);
                y = row(cs, regular, y, "Estimated annual savings",
                        inr(p.estimatedAnnualSavings()), false);
                y = row(cs, regular, y, "Estimated payback",
                        p.estimatedPaybackYears() == null
                                ? "Not available"
                                : plain(p.estimatedPaybackYears()) + " years", false);
                y -= 12;

                // ---- assumptions (wrapped) ----
                y = section(cs, bold, y, "Important assumptions");
                List<String> notes = List.of(
                        "Generation assumed at " + plain(s.annualGenerationPerKw())
                                + " units per kW per year; roof space assumed at "
                                + plain(s.roofAreaPerKw()) + " sq ft per kW.",
                        "Subsidy: " + s.subsidyDisclaimer(),
                        s.pricingDisclaimer()
                );
                for (String note : notes) {
                    List<String> lines = wrap(note, regular, 9, CONTENT_W - 12);
                    for (int i = 0; i < lines.size(); i++) {
                        if (i == 0) text(cs, regular, 9, MARGIN, y, "-");
                        text(cs, regular, 9, MARGIN + 12, y, lines.get(i));
                        y -= 12;
                    }
                    y -= 4;
                }

                // ---- footer ----
                cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
                text(cs, regular, 8, MARGIN, MARGIN - 10,
                        "Generated by SolarFlow. All figures are estimates based on the rules effective on the quotation date.");
                cs.setNonStrokingColor(0f, 0f, 0f);
            }

            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate quotation PDF", e);
        }
    }

    // ---------------- drawing helpers ----------------

    private float section(PDPageContentStream cs, PDType1Font bold, float y, String title) throws IOException {
        text(cs, bold, 12, MARGIN, y, title);
        return y - 20;
    }

    private float row(PDPageContentStream cs, PDType1Font font, float y,
                      String label, String value, boolean emphasise) throws IOException {
        float size = emphasise ? 12 : 10.5f;
        text(cs, font, size, MARGIN, y, label);
        float w = font.getStringWidth(clean(value)) / 1000f * size;
        text(cs, font, size, MARGIN + CONTENT_W - w, y, value);

        cs.setStrokingColor(0.85f, 0.85f, 0.85f);
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y - 5);
        cs.lineTo(MARGIN + CONTENT_W, y - 5);
        cs.stroke();
        return y - 19;
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size,
                      float x, float y, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(clean(value));
        cs.endText();
    }

    private List<String> wrap(String value, PDType1Font font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : clean(value).split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.getStringWidth(candidate) / 1000f * size > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // ---------------- formatting helpers ----------------

    /** Standard-14 fonts cannot encode every character (the rupee sign, for one). Keep ASCII only. */
    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", " ");
    }

    /** "RECOMMENDED" -> "Recommended", "USAGE" -> "Usage". */
    private static String pretty(String v) {
        if (v == null || v.isBlank()) return "-";
        String t = v.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String plain(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String units(BigDecimal v) {
        return v == null ? "-" : group(v.setScale(0, RoundingMode.HALF_UP).toPlainString());
    }

    /** 181000 -> "INR 1,81,000.00" (Indian digit grouping). */
    private static String inr(BigDecimal v) {
        if (v == null) return "-";
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP);
        String[] parts = r.abs().toPlainString().split("\\.");
        return (r.signum() < 0 ? "-" : "") + "INR " + group(parts[0]) + "." + parts[1];
    }

    private static String group(String digits) {
        if (digits.length() <= 3) return digits;
        String last3 = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            if (i > 0 && (rest.length() - i) % 2 == 0) sb.append(',');
            sb.append(rest.charAt(i));
        }
        return sb + "," + last3;
    }
}