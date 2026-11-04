package com.solarFlow.solarFlow.qualification.dto;

import com.solarFlow.solarFlow.qualification.domain.NextAction;

import java.util.List;

public record QualificationOutcome (int score, NextAction nextAction, List<QualificationReason> reasons, List<QualificationGate> gates
){
}
