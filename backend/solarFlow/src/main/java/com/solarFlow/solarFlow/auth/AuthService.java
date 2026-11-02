package com.solarFlow.solarFlow.auth;


import com.solarFlow.solarFlow.user.Role;
import com.solarFlow.solarFlow.user.User;
import com.solarFlow.solarFlow.user.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo users;
    private final PasswordEncoder encoder;
    private  final JwtService jwtService ;

    @Transactional
    public UserResponseDTO register(RegisterRequestDTO req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }



        User user = User.builder()
                .email(email)
                .fullName(req.fullName().trim())
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.HOMEOWNER)
                .consentAt(Instant.now())
                .consentVersion("v1")
                .build();

        User saved = users.save(user);
        return new UserResponseDTO(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole().name());
    }

    public LoginResponseDTO login (LoginRequestDTO request){
        User user = users.findByEmail(request.email().trim().toLowerCase())
                .filter(User::isActive)
                .filter(u->encoder.matches(request.password() , u.getPasswordHash()))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid User or Password "));

        return new LoginResponseDTO(jwtService.generate(user), "Bearer", jwtService.expirationInSecond(),
                new UserResponseDTO(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name()));

    }
}