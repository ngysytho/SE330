package com.example.chatapp.controller;

import com.example.chatapp.dto.AuthDtos.AuthResponse;
import com.example.chatapp.dto.AuthDtos.LoginRequest;
import com.example.chatapp.dto.AuthDtos.RegisterRequest;
import com.example.chatapp.dto.AuthDtos.UserResponse;
import com.example.chatapp.security.SecurityUtils;
import com.example.chatapp.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public void logout() {
        authService.logout(SecurityUtils.currentUserId());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(authService.me(SecurityUtils.currentUser()));
    }
}
