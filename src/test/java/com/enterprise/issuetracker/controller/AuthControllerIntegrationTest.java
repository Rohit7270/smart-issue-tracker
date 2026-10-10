package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.LoginRequest;
import com.enterprise.issuetracker.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.enterprise.issuetracker.entity.RoleType;
import com.enterprise.issuetracker.entity.User;
import com.enterprise.issuetracker.repository.UserRepository;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testRegisterUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @Test
    public void testRegisterUser_CannotSelfAssignAdminOrManager() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("unauthorizedadmin");
        request.setEmail("unauthorizedadmin@example.com");
        request.setPassword("password123");
        request.setFullName("Untrusted User");
        request.setRoles(Set.of("ADMIN", "MANAGER"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        User registeredUser = userRepository.findByUsername("unauthorizedadmin")
                .orElseThrow(() -> new AssertionError("User should have been created in database"));

        assertEquals(1, registeredUser.getRoles().size());
        assertEquals(RoleType.ROLE_USER, registeredUser.getRoles().iterator().next().getName());
    }

    @Test
    public void testRegisterUser_ValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("u"); // username size must be min 3
        request.setEmail("invalid-email");
        request.setPassword("123"); // password size must be min 6
        request.setFullName("");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    public void testLogin_Success() throws Exception {
        // User 'developer' is seeded in data-test.sql with password 'password'
        LoginRequest request = new LoginRequest();
        request.setUsername("developer");
        request.setPassword("password");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    public void testLogin_Failure() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("developer");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Authentication failed")));
    }
}
