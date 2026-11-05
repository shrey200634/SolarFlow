package com.solarFlow.solarFlow.quotation.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.lead.domain.Lead;
import com.solarFlow.solarFlow.lead.domain.LeadStatus;
import com.solarFlow.solarFlow.lead.repo.LeadRepo;
import com.solarFlow.solarFlow.lead.service.LeadService;
import com.solarFlow.solarFlow.pricing.dto.PricingCalculationDTO;
import com.solarFlow.solarFlow.pricing.service.PricingCalculationService;
import com.solarFlow.solarFlow.quotation.domain.Quotation;
import com.solarFlow.solarFlow.quotation.domain.QuotationStatus;
import com.solarFlow.solarFlow.quotation.dto.CreateQuotationRequestDTO;
import com.solarFlow.solarFlow.quotation.dto.QuotationSnapshot;
import com.solarFlow.solarFlow.quotation.repo.QuotationRepo;
import com.solarFlow.solarFlow.solar.dto.SolarRecommendationDTO;
import com.solarFlow.solarFlow.solar.service.SolarSizingProperties;
import com.solarFlow.solarFlow.solar.service.SolarSizingService;
import com.solarFlow.solarFlow.user.domain.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepo quotationRepo;

    @Mock
    private LeadRepo leadRepo;

    @Mock
    private LeadService leadService;

    @Mock
    private SolarSizingService solarSizingService;

    @Mock
    private PricingCalculationService pricingCalculationService;

    @Mock
    private SolarSizingProperties solarSizingProperties;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private QuotationPdfService quotationPdfService;

    @InjectMocks
    private QuotationService quotationService;

    private UUID leadId;
    private UUID quotationId;

    private Lead lead;
    private AuthUser salesActor;
    private AuthUser homeownerActor;

    private SolarRecommendationDTO sizing;
    private PricingCalculationDTO pricing;
    private QuotationSnapshot snapshot;

    @BeforeEach
    void setUp() {

        leadId = UUID.randomUUID();
        quotationId = UUID.randomUUID();

        lead = Lead.builder()
                .id(leadId)
                .status(LeadStatus.BILL_CONFIRMED)
                .build();

        salesActor = new AuthUser(UUID.randomUUID(), Role.SALES);
        homeownerActor = new AuthUser(UUID.randomUUID(), Role.HOMEOWNER);

        sizing = new SolarRecommendationDTO(
                new BigDecimal("340"),
                new BigDecimal("350"),
                new BigDecimal("5"),
                new BigDecimal("2.91"),
                new BigDecimal("3.5"),
                new BigDecimal("2.5"),
                "USAGE",
                true,
                "Recommended system",
                List.of(
                        new SolarRecommendationDTO.SolarOptionDTO(
                                "ECONOMY",
                                new BigDecimal("1.5"),
                                new BigDecimal("2100")
                        ),
                        new SolarRecommendationDTO.SolarOptionDTO(
                                "RECOMMENDED",
                                new BigDecimal("2.5"),
                                new BigDecimal("3500")
                        ),
                        new SolarRecommendationDTO.SolarOptionDTO(
                                "MAXIMUM",
                                new BigDecimal("3.0"),
                                new BigDecimal("4200")
                        )
                )
        );

        pricing = new PricingCalculationDTO(
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

        snapshot = new QuotationSnapshot(
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
    }

    private void prepareValidCreation() throws Exception {

        when(leadService.findAccessible(leadId, salesActor))
                .thenReturn(lead);

        when(solarSizingService.recommend(leadId, salesActor))
                .thenReturn(sizing);

        when(pricingCalculationService.calculate(
                new BigDecimal("2.5")))
                .thenReturn(pricing);

        when(solarSizingProperties.getAnnualGenerationPerKw())
                .thenReturn(new BigDecimal("1400"));

        when(solarSizingProperties.getRoofAreaPerKw())
                .thenReturn(new BigDecimal("100"));

        when(jsonMapper.writeValueAsString(any(QuotationSnapshot.class)))
                .thenReturn("snapshot-json");

        when(jsonMapper.readValue(
                eq("snapshot-json"),
                eq(QuotationSnapshot.class)))
                .thenReturn(snapshot);

        when(quotationRepo.save(any(Quotation.class)))
                .thenAnswer(invocation -> {
                    Quotation quotation = invocation.getArgument(0);
                    quotation.setId(quotationId);
                    quotation.setCreatedAt(Instant.now());
                    return quotation;
                });
    }

    private Quotation existingQuotation(QuotationStatus status) {

        return Quotation.builder()
                .id(quotationId)
                .lead(lead)
                .selectedCapacityKw(new BigDecimal("2.5"))
                .selectedOptionType("RECOMMENDED")
                .snapshotJson("snapshot-json")
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void salesCanCreateQuotation() throws Exception {

        prepareValidCreation();

        var response = quotationService.create(
                leadId,
                new CreateQuotationRequestDTO(new BigDecimal("2.5")),
                salesActor
        );

        assertNotNull(response);
        assertEquals(quotationId, response.id());
        assertEquals(leadId, response.leadId());
        assertEquals(0,
                new BigDecimal("2.5").compareTo(response.selectedCapacityKw()));
        assertEquals("RECOMMENDED", response.selectedOptionType());
        assertEquals(QuotationStatus.ISSUED, response.status());

        assertEquals(LeadStatus.QUOTED, lead.getStatus());

        verify(quotationRepo).save(any(Quotation.class));
        verify(leadRepo).save(lead);
        verify(pricingCalculationService).calculate(
                new BigDecimal("2.5"));
    }

    @Test
    void quotationStoresSnapshotAtCreation() throws Exception {

        prepareValidCreation();

        quotationService.create(
                leadId,
                new CreateQuotationRequestDTO(new BigDecimal("2.5")),
                salesActor
        );

        verify(jsonMapper).writeValueAsString(argThat(savedSnapshot ->
                savedSnapshot instanceof QuotationSnapshot
                        && ((QuotationSnapshot) savedSnapshot)
                        .pricing().netSystemCost()
                        .compareTo(new BigDecimal("181000.00")) == 0
        ));

        verify(quotationRepo).save(argThat(quotation ->
                "snapshot-json".equals(quotation.getSnapshotJson())
                        && quotation.getStatus() == QuotationStatus.ISSUED
        ));
    }

    @Test
    void rejectsCapacityThatIsNotRecommended() {

        when(leadService.findAccessible(leadId, salesActor))
                .thenReturn(lead);

        when(solarSizingService.recommend(leadId, salesActor))
                .thenReturn(sizing);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.create(
                        leadId,
                        new CreateQuotationRequestDTO(
                                new BigDecimal("4.0")),
                        salesActor
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(quotationRepo, never()).save(any());
    }

    @Test
    void rejectsNonViableLead() {

        SolarRecommendationDTO nonViable = new SolarRecommendationDTO(
                sizing.monthlyUnits(),
                sizing.roofAreaSqft(),
                sizing.sanctionedLoadKw(),
                sizing.usageLimitKw(),
                sizing.roofLimitKw(),
                new BigDecimal("0.5"),
                "USAGE",
                false,
                "Not viable",
                List.of()
        );

        when(leadService.findAccessible(leadId, salesActor))
                .thenReturn(lead);

        when(solarSizingService.recommend(leadId, salesActor))
                .thenReturn(nonViable);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.create(
                        leadId,
                        new CreateQuotationRequestDTO(
                                new BigDecimal("0.5")),
                        salesActor
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(quotationRepo, never()).save(any());
    }

    @Test
    void homeownerCannotCreateQuotation() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.create(
                        leadId,
                        new CreateQuotationRequestDTO(
                                new BigDecimal("2.5")),
                        homeownerActor
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(leadService, never()).findAccessible(any(), any());
        verify(quotationRepo, never()).save(any());
    }

    @Test
    void homeownerCanAcceptQuotation() {

        Quotation quotation = existingQuotation(QuotationStatus.ISSUED);

        when(quotationRepo.findById(quotationId))
                .thenReturn(Optional.of(quotation));

        when(leadService.findAccessible(leadId, homeownerActor))
                .thenReturn(lead);

        when(quotationRepo.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(jsonMapper.readValue(
                eq("snapshot-json"),
                eq(QuotationSnapshot.class)))
                .thenReturn(snapshot);

        var response = quotationService.accept(
                quotationId,
                homeownerActor
        );

        assertEquals(QuotationStatus.ACCEPTED, response.status());
        assertNotNull(response.acceptedAt());
        assertEquals(LeadStatus.ACCEPTED, lead.getStatus());

        verify(quotationRepo).save(quotation);
        verify(leadRepo).save(lead);
    }

    @Test
    void repeatedAcceptanceIsIdempotent() {

        Quotation quotation = existingQuotation(QuotationStatus.ACCEPTED);
        Instant originalAcceptedAt = Instant.parse("2026-11-01T10:00:00Z");
        quotation.setAcceptedAt(originalAcceptedAt);

        when(quotationRepo.findById(quotationId))
                .thenReturn(Optional.of(quotation));

        when(leadService.findAccessible(leadId, homeownerActor))
                .thenReturn(lead);

        when(jsonMapper.readValue(
                eq("snapshot-json"),
                eq(QuotationSnapshot.class)))
                .thenReturn(snapshot);

        var response = quotationService.accept(
                quotationId,
                homeownerActor
        );

        assertEquals(QuotationStatus.ACCEPTED, response.status());
        assertEquals(originalAcceptedAt, response.acceptedAt());

        verify(quotationRepo, never()).save(any());
        verify(leadRepo, never()).save(any());
    }

    @Test
    void salesCannotAcceptQuotation() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.accept(quotationId, salesActor)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(quotationRepo, never()).findById(any());
    }

    @Test
    void homeownerCannotAccessAnotherUsersQuotation() {

        Quotation quotation = existingQuotation(QuotationStatus.ISSUED);

        when(quotationRepo.findById(quotationId))
                .thenReturn(Optional.of(quotation));

        when(leadService.findAccessible(leadId, homeownerActor))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lead not found"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.get(
                        quotationId,
                        homeownerActor
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void downloadingPdfUsesSavedSnapshotWithoutRecalculatingPricing() {

        Quotation quotation = existingQuotation(QuotationStatus.ISSUED);
        byte[] expectedPdf = new byte[]{1, 2, 3, 4};

        when(quotationRepo.findById(quotationId))
                .thenReturn(Optional.of(quotation));

        when(leadService.findAccessible(leadId, homeownerActor))
                .thenReturn(lead);

        when(jsonMapper.readValue(
                eq("snapshot-json"),
                eq(QuotationSnapshot.class)))
                .thenReturn(snapshot);

        when(quotationPdfService.generate(snapshot))
                .thenReturn(expectedPdf);

        byte[] result = quotationService.downloadPdf(
                quotationId,
                homeownerActor
        );

        assertArrayEquals(expectedPdf, result);

        verify(quotationPdfService).generate(snapshot);
        verify(pricingCalculationService, never()).calculate(any());
    }

    @Test
    void missingQuotationReturnsNotFound() {

        when(quotationRepo.findById(quotationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.get(
                        quotationId,
                        homeownerActor
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void cannotCreateQuotationForLeadThatIsAlreadyAccepted() {

        lead.setStatus(LeadStatus.ACCEPTED);

        when(leadService.findAccessible(leadId, salesActor))
                .thenReturn(lead);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.create(
                        leadId,
                        new CreateQuotationRequestDTO(new BigDecimal("2.5")),
                        salesActor
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(LeadStatus.ACCEPTED, lead.getStatus());

        verify(quotationRepo, never()).save(any());
    }

    @Test
    void cannotAcceptSecondQuotationOnSameLead() {

        lead.setStatus(LeadStatus.ACCEPTED);

        Quotation otherQuotation = existingQuotation(QuotationStatus.ISSUED);

        when(quotationRepo.findById(quotationId))
                .thenReturn(Optional.of(otherQuotation));

        when(leadService.findAccessible(leadId, homeownerActor))
                .thenReturn(lead);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quotationService.accept(quotationId, homeownerActor)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(quotationRepo, never()).save(any());
        verify(leadRepo, never()).save(any());
    }
}