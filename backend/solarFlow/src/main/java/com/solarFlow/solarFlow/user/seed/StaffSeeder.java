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

        // Each account is checked on its own, so a SALES user is still created
        // on a database where the admin already exists.
        seedIfMissing("admin@solarflow.local", "SolarFlow Admin", Role.ADMIN);
        seedIfMissing("sales@solarflow.local", "SolarFlow Sales", Role.SALES);
    }

    private void seedIfMissing(String email, String fullName, Role role) {

        if (userRepo.existsByEmail(email)) {
            log.info("{} account already exists. Skipping.", role);
            return;
        }

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .role(role)
                .active(true)
                .build();

        userRepo.save(user);

        log.info("Initial {} account created: {}", role, email);
    }
}