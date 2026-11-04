package com.solarFlow.solarFlow.qualification.service;

import com.solarFlow.solarFlow.qualification.dto.QualificationRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QualificationConfig {

    @Bean
    public QualificationRules qualificationRules(QualificationProperties properties) {
        return properties.toRules();
    }
}