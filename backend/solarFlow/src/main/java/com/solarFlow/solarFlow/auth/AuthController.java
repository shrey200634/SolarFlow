package com.solarFlow.solarFlow.auth;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService ;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register (@Valid @RequestBody RegisterRequestDTO req ){
        UserResponseDTO user = authService.register(req);
        return  ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login (@Valid @RequestBody LoginRequestDTO requestDTO){

        LoginResponseDTO response = authService.login(requestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
