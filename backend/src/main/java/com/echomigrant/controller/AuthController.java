package com.echomigrant.controller;

import com.echomigrant.dto.UtilisateurDto;
import com.echomigrant.dto.auth.AuthResponse;
import com.echomigrant.dto.auth.LoginRequest;
import com.echomigrant.dto.auth.RegisterRequest;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UtilisateurDto> me(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(UtilisateurDto.from(utilisateur));
    }
}
