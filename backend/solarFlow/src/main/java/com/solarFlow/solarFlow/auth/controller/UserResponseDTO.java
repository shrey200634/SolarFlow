package com.solarFlow.solarFlow.auth.controller;

import java.util.UUID;

public record UserResponseDTO(
        UUID id ,
        String fullName ,
        String email ,
        String role
) {
}
