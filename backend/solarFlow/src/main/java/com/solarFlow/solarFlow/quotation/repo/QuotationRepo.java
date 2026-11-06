package com.solarFlow.solarFlow.quotation.repo;

import com.solarFlow.solarFlow.quotation.domain.Quotation;
import com.solarFlow.solarFlow.quotation.domain.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuotationRepo extends JpaRepository<Quotation, UUID> {

    boolean existsByLeadIdAndStatus(UUID leadId, QuotationStatus status);

    List<Quotation> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}