package com.solarFlow.solarFlow.user.controller;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.auth.controller.UserResponseDTO;
import com.solarFlow.solarFlow.user.domain.User;
import com.solarFlow.solarFlow.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepo users;

    @GetMapping("/me")
    public UserResponseDTO me(@AuthenticationPrincipal AuthUser me) {
        User u = users.findById(me.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new UserResponseDTO(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
    }
}