package com.solarFlow.solarFlow.auth;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder

public record RegisterRequestDTO(

        @NotBlank @Size(max = 120)
        String fullName ,

        @NotBlank @Email @Size(max = 255)
        String email ,

        @Pattern(regexp = "^[0-9]{10}$", message = "must be a 10-digit mobile number")
        String phone,

        @NotBlank @Size(min = 8, max = 72)
        String password ,

        @AssertTrue(message = "consent is required")
        boolean consent
) {
}
