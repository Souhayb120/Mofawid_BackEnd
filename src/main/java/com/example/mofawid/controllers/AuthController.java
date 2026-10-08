package com.example.mofawid.controllers;

import com.example.mofawid.DTO.UserDTO.AuthResponse;
import com.example.mofawid.DTO.UserDTO.UserLoginDTO;
import com.example.mofawid.services.ServicesImpl.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody UserLoginDTO dto
    ) {
        return ResponseEntity.ok(
                authService.login(dto)
        );
    }
}