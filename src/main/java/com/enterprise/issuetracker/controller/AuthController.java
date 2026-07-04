package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.*;
import com.enterprise.issuetracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for User Registration, Login, and JWT Token Refreshing")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user in the system", description = "Creates a user account. Default role assigned is ROLE_USER.")
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        ApiResponse<String> response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Verifies username and password, then returns a stateless JWT Access Token and Refresh Token.")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        ApiResponse<JwtResponse> response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token", description = "Generates a new access token if the provided refresh token is valid and unexpired.")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshJwtToken(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        ApiResponse<TokenRefreshResponse> response = authService.refreshJwtToken(refreshRequest);
        return ResponseEntity.ok(response);
    }
}
