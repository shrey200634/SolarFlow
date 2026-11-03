package com.solarFlow.solarFlow.bill.repo;

import com.solarFlow.solarFlow.bill.domain.LeadDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadDocumentRepo extends JpaRepository<LeadDocument, UUID> {

    List<LeadDocument>  findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
