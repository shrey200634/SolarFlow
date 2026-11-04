package com.solarFlow.solarFlow.solar.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "solarflow.solar-sizing")
public class SolarSizingProperties {

    private BigDecimal annualGenerationPerKw = new BigDecimal("1400");

    private BigDecimal roofAreaPerKw = new BigDecimal("100");

    private BigDecimal economyFactor = new BigDecimal("0.70");

    private BigDecimal capacityStep = new BigDecimal("0.5");
}