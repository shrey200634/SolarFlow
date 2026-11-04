package com.solarFlow.solarFlow.qualification.dto;

public record QualificationReason (
        String code, int points, int maxPoints, boolean passed, String text
){
}
