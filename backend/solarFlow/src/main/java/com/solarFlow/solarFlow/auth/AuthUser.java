package com.solarFlow.solarFlow.auth;

import com.solarFlow.solarFlow.user.Role;

import java.util.UUID;

public record AuthUser(
        UUID id ,
        Role role
) {
}
