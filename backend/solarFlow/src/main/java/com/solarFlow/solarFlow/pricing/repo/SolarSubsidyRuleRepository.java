package com.solarFlow.solarFlow.pricing.repo;

import com.solarFlow.solarFlow.pricing.domain.SolarSubsidyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SolarSubsidyRuleRepository
        extends JpaRepository<SolarSubsidyRule, UUID> {

    @Query("""
            SELECT r FROM SolarSubsidyRule r
            WHERE r.active = true
              AND r.effectiveFrom <= :date
              AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)
            ORDER BY r.effectiveFrom DESC
            """)
    List<SolarSubsidyRule> findApplicableRules(
            @Param("date") LocalDate date
    );


}