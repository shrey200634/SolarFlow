package com.solarFlow.solarFlow.lead.repo;

import com.solarFlow.solarFlow.lead.domain.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepo extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    boolean existsByPhoneE164(String phoneE164);

    List<Lead> findByPhoneE164(String phoneE164);

    // Loads owner and assignee in the same query so the list does not run one query per row.
    @Override
    @EntityGraph(attributePaths = {"owner", "assignedTo"})
    Page<Lead> findAll(Specification<Lead> spec, Pageable pageable);

    @Query("select l.status, count(l) from Lead l group by l.status")
    List<Object[]> countGroupedByStatus();
}