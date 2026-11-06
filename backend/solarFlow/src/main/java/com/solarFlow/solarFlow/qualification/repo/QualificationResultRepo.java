package com.solarFlow.solarFlow.qualification.repo;

import com.solarFlow.solarFlow.qualification.domain.QualificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualificationResultRepo extends JpaRepository<QualificationResult, UUID> {

    Optional<QualificationResult> findFirstByLeadIdOrderByCreatedAtDesc(UUID leadId);

    @Query("""
            select q from QualificationResult q
            where q.lead.id in :leadIds
              and q.createdAt = (select max(q2.createdAt) from QualificationResult q2
                                 where q2.lead.id = q.lead.id)
            """)
    List<QualificationResult> findLatestForLeads(@Param("leadIds") Collection<UUID> leadIds);
}