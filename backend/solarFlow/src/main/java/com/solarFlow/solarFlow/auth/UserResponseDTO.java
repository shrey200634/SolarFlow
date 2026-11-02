package com.solarFlow.solarFlow.auth;

import java.util.UUID;

public record UserResponseDTO(
        UUID id ,
        String fullName ,
        String email ,
        String role
) {
}
