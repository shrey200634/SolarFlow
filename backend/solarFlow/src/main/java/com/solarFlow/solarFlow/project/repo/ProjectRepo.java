package com.solarFlow.solarFlow.project.repo;

import com.solarFlow.solarFlow.project.domain.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepo extends JpaRepository<Project, UUID> {

    boolean existsByQuotationId(UUID quotationId);

    Optional<Project> findByStatusToken(String statusToken);

    // staff view: newest first
    List<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // homeowner view: only projects on leads they own
    List<Project> findByLeadOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Project> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}