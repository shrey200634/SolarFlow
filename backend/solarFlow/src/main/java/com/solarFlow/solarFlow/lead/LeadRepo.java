package com.solarFlow.solarFlow.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepo extends JpaRepository<Lead , UUID> {
    boolean existsByPhoneE164( String phoneE164);
    List<Lead> findByPhoneE164(String phoneE164);

}
