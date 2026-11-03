package com.solarFlow.solarFlow.auth.security;

import com.solarFlow.solarFlow.user.domain.Role;

import java.util.UUID;

public record AuthUser(
        UUID id ,
        Role role
) {
}
