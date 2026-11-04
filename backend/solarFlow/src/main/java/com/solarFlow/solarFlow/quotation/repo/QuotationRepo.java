package com.solarFlow.solarFlow.quotation.repo;

import com.solarFlow.solarFlow.quotation.domain.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuotationRepo extends JpaRepository<Quotation, UUID> {
}