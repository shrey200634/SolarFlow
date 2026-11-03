package com.solarFlow.solarFlow.user.seed;

import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.domain.User;
import com.solarFlow.solarFlow.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffSeeder implements ApplicationRunner {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password:}")
    private String seedPassword;

    @Override
    public void run(ApplicationArguments args) {

        if (seedPassword == null || seedPassword.isBlank()) {
            log.info("Staff seeding skipped: SEED_PASSWORD is not configured.");
            return;
        }

        String adminEmail = "admin@solarflow.local";

        if (userRepo.existsByEmail(adminEmail)) {
            log.info("Admin account already exists. Skipping staff seeding.");
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .fullName("SolarFlow Admin")
                .passwordHash(passwordEncoder.encode(seedPassword))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepo.save(admin);

        log.info("Initial admin account created successfully.");
    }
}