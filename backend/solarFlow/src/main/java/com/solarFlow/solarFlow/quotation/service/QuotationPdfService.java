package com.solarFlow.solarFlow.quotation.service;

import com.solarFlow.solarFlow.quotation.dto.QuotationSnapshot;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QuotationPdfService {

    public byte[] generate(QuotationSnapshot snapshot) {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font bold =
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDType1Font regular =
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                content.beginText();
                content.setFont(bold, 20);
                content.newLineAtOffset(50, 750);
                content.showText("SOLARFLOW - SOLAR QUOTATION");

                content.setFont(regular, 10);
                content.newLineAtOffset(0, -25);
                content.showText("Quotation date: " + snapshot.quotationDate());

                content.newLineAtOffset(0, -18);
                content.showText("Lead reference: " + snapshot.leadId());

                content.newLineAtOffset(0, -35);
                content.setFont(bold, 14);
                content.showText("Recommended System");

                content.setFont(regular, 11);
                content.newLineAtOffset(0, -22);
                content.showText("Selected option: " + snapshot.selectedOptionType());

                content.newLineAtOffset(0, -18);
                content.showText("System capacity: "
                        + snapshot.selectedCapacityKw() + " kW");

                content.newLineAtOffset(0, -18);
                content.showText("Estimated yearly generation: "
                        + snapshot.pricing().annualGenerationUnits() + " units");

                content.newLineAtOffset(0, -35);
                content.setFont(bold, 14);
                content.showText("Cost Breakdown");

                content.setFont(regular, 11);

                content.newLineAtOffset(0, -22);
                content.showText("Price per kW: INR "
                        + snapshot.pricing().pricePerKw());

                content.newLineAtOffset(0, -18);
                content.showText("Gross system cost: INR "
                        + snapshot.pricing().grossSystemCost());

                content.newLineAtOffset(0, -18);
                content.showText("Estimated subsidy: INR "
                        + snapshot.pricing().subsidyAmount());

                content.newLineAtOffset(0, -18);
                content.showText("Net system cost: INR "
                        + snapshot.pricing().netSystemCost());

                content.newLineAtOffset(0, -18);
                content.showText("Estimated annual savings: INR "
                        + snapshot.pricing().estimatedAnnualSavings());

                content.newLineAtOffset(0, -18);
                content.showText("Estimated payback: "
                        + (snapshot.pricing().estimatedPaybackYears() == null
                        ? "Not available"
                        : snapshot.pricing().estimatedPaybackYears() + " years"));

                content.newLineAtOffset(0, -35);
                content.setFont(bold, 12);
                content.showText("Important Assumptions");

                content.setFont(regular, 9);

                content.newLineAtOffset(0, -20);
                content.showText("Generation factor: "
                        + snapshot.annualGenerationPerKw()
                        + " units per kW per year");

                content.newLineAtOffset(0, -16);
                content.showText("Roof area assumption: "
                        + snapshot.roofAreaPerKw() + " sq ft per kW");

                content.newLineAtOffset(0, -22);
                content.showText("Subsidy: " + snapshot.subsidyDisclaimer());

                content.newLineAtOffset(0, -28);
                content.showText(snapshot.pricingDisclaimer());

                content.newLineAtOffset(0, -40);
                content.setFont(bold, 10);
                content.showText("SolarFlow - Build. Create. Grow.");

                content.endText();
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to generate quotation PDF", exception
            );
        }
    }
}