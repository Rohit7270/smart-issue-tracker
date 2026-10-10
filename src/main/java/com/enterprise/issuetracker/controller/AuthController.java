package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.*;
import com.enterprise.issuetracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user registration, login, and JWT token refreshing.
 * Matches your Spring_Security project flow!
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1. Register a new user account
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        ApiResponse<String> response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Authenticate user credentials and return JWT tokens
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        ApiResponse<JwtResponse> response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    // 3. Refresh access token using a valid refresh token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshJwtToken(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        ApiResponse<TokenRefreshResponse> response = authService.refreshJwtToken(refreshRequest);
        return ResponseEntity.ok(response);
    }
}
