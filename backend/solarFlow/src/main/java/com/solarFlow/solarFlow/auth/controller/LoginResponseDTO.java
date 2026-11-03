package com.solarFlow.solarFlow.auth.controller;

public record LoginResponseDTO(

        String accessToken ,
        String tokenType ,
        long expiresInSeconds ,
        UserResponseDTO user
) {
}
