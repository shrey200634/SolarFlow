package com.solarFlow.solarFlow.lead.controller;

import com.solarFlow.solarFlow.lead.domain.LeadStatus;

import java.util.Map;

public record LeadStatsDTO(long total, Map<LeadStatus, Long> byStatus) { }