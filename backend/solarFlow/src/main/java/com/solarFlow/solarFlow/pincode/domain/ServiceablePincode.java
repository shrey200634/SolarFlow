package com.solarFlow.solarFlow.pincode.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "serviceable_pincodes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_serviceable_pincode", columnNames = "pincode")
})
public class ServiceablePincode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 6, columnDefinition = "char(6)")
    private String pincode;

    @Column(name = "area_name", length = 100)
    private String areaName;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}