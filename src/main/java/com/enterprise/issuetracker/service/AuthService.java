package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.JwtResponse;
import com.enterprise.issuetracker.dto.LoginRequest;
import com.enterprise.issuetracker.dto.RegisterRequest;
import com.enterprise.issuetracker.dto.TokenRefreshRequest;
import com.enterprise.issuetracker.dto.TokenRefreshResponse;

public interface AuthService {
    ApiResponse<JwtResponse> authenticateUser(LoginRequest loginRequest);
    ApiResponse<String> registerUser(RegisterRequest registerRequest);
    ApiResponse<TokenRefreshResponse> refreshJwtToken(TokenRefreshRequest refreshRequest);
}
