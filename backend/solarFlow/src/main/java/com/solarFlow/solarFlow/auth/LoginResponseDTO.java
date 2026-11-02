package com.solarFlow.solarFlow.auth;

public record LoginResponseDTO(

        String accessToken ,
        String tokenType ,
        long expiresInSeconds ,
        UserResponseDTO user
) {
}
