package com.solarFlow.solarFlow.quotation.service;

import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.quotation.dto.QuotationSnapshot;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuotationPdfServiceTest {

    private final QuotationPdfService pdfService =
            new QuotationPdfService();

    @Test
    void generatesReadableQuotationPdf() throws Exception {

        UUID leadId = UUID.randomUUID();

        SolarRecommendationDTO sizing = new SolarRecommendationDTO(
                new BigDecimal("340"),
                new BigDecimal("350"),
                new BigDecimal("5"),
                new BigDecimal("2.91"),
                new BigDecimal("3.5"),
                new BigDecimal("2.5"),
                "USAGE",
                true,
                "Recommended system",
                List.of()
        );

        PricingCalculationDTO pricing = new PricingCalculationDTO(
                new BigDecimal("2.5"),
                new BigDecimal("100000"),
                new BigDecimal("250000.00"),
                new BigDecimal("69000.00"),
                new BigDecimal("181000.00"),
                new BigDecimal("3500"),
                new BigDecimal("8"),
                new BigDecimal("28000.00"),
                new BigDecimal("6.46"),
                true,
                LocalDate.now(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        QuotationSnapshot snapshot = new QuotationSnapshot(
                leadId,
                LocalDate.now(),
                "RECOMMENDED",
                new BigDecimal("2.5"),
                sizing,
                new BigDecimal("1400"),
                new BigDecimal("100"),
                pricing,
                "Subsidy is an estimate.",
                "Pricing and payback are estimates."
        );

        byte[] pdfBytes = pdfService.generate(snapshot);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            assertEquals(1, document.getNumberOfPages());

            String text = new PDFTextStripper().getText(document);

            assertTrue(text.contains("SOLARFLOW"));
            assertTrue(text.contains("SOLAR QUOTATION"));
            assertTrue(text.contains("RECOMMENDED"));
            assertTrue(text.contains("2.5"));
            assertTrue(text.contains("250000.00"));
            assertTrue(text.contains("69000.00"));
            assertTrue(text.contains("181000.00"));
            assertTrue(text.contains("28000.00"));
            assertTrue(text.contains("6.46"));
            assertTrue(text.contains("Subsidy is an estimate."));
        }
    }
}