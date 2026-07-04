package com.enterprise.issuetracker.service.impl;

import com.enterprise.issuetracker.dto.*;
import com.enterprise.issuetracker.entity.Role;
import com.enterprise.issuetracker.entity.RoleType;
import com.enterprise.issuetracker.entity.User;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.repository.RoleRepository;
import com.enterprise.issuetracker.repository.UserRepository;
import com.enterprise.issuetracker.security.CustomUserDetails;
import com.enterprise.issuetracker.security.JwtTokenProvider;
import com.enterprise.issuetracker.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public ApiResponse<JwtResponse> authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        JwtResponse jwtResponse = new JwtResponse(
                accessToken,
                refreshToken,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return ApiResponse.success(jwtResponse, "User authenticated successfully");
    }

    @Override
    @Transactional
    public ApiResponse<String> registerUser(RegisterRequest registerRequest) {
        log.info("Registering user: {}", registerRequest.getUsername());

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email is already in use!");
        }

        // Create new user's account
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .active(true)
                .build();

        Set<String> strRoles = registerRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                    .orElseThrow(() -> new ResourceNotFoundException("Error: Default Role is not found in database."));
            roles.add(userRole);
        } else {
            strRoles.forEach(roleName -> {
                switch (roleName.toUpperCase()) {
                    case "ADMIN":
                        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                                .orElseThrow(() -> new ResourceNotFoundException("Error: Role Admin is not found."));
                        roles.add(adminRole);
                        break;
                    case "MANAGER":
                        Role managerRole = roleRepository.findByName(RoleType.ROLE_MANAGER)
                                .orElseThrow(() -> new ResourceNotFoundException("Error: Role Manager is not found."));
                        roles.add(managerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                                .orElseThrow(() -> new ResourceNotFoundException("Error: Role User is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ApiResponse.success("User registered successfully!");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<TokenRefreshResponse> refreshJwtToken(TokenRefreshRequest refreshRequest) {
        String requestRefreshToken = refreshRequest.getRefreshToken();

        if (jwtTokenProvider.validateJwtToken(requestRefreshToken)) {
            String username = jwtTokenProvider.getUsernameFromJwtToken(requestRefreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for the given refresh token"));

            if (!user.isActive()) {
                throw new BadRequestException("This user account is inactive.");
            }

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            String newAccessToken = jwtTokenProvider.generateAccessTokenFromUsername(
                    user.getUsername(), user.getId(), user.getEmail(), roles
            );

            TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken, requestRefreshToken);
            return ApiResponse.success(response, "Access token refreshed successfully");
        }

        throw new BadRequestException("Invalid or expired Refresh Token");
    }
}
