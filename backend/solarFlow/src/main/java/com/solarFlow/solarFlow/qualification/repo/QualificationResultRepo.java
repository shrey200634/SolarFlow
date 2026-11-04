package com.solarFlow.solarFlow.qualification.repo;

import com.solarFlow.solarFlow.qualification.domain.QualificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualificationResultRepo extends JpaRepository<QualificationResult, UUID> {

    Optional<QualificationResult> findFirstByLeadIdOrderByCreatedAtDesc(UUID leadId);
}