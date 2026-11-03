package com.solarFlow.solarFlow.bill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillExtractionRepo extends JpaRepository<BillExtraction, UUID> {

    Optional<BillExtraction> findFirstByDocumentIdOrderByAttemptDesc(UUID documentId);

    boolean existsByDocumentIdAndAttempt(UUID documentId, int attempt);
}