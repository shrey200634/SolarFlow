package com.solarFlow.solarFlow.bill.repo;

import com.solarFlow.solarFlow.bill.domain.DocumentStatus;
import com.solarFlow.solarFlow.bill.domain.LeadDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadDocumentRepo extends JpaRepository<LeadDocument, UUID> {

    List<LeadDocument>  findByLeadIdOrderByCreatedAtDesc(UUID leadId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update LeadDocument d set d.status = :next where d.id = :id and d.status = :expected")
    int transition(@Param("id") UUID id,
                   @Param("expected") DocumentStatus expected,
                   @Param("next") DocumentStatus next);
}
