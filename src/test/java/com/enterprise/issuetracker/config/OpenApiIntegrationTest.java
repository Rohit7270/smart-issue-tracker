package com.enterprise.issuetracker.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify OpenAPI Spec - No duplicate /api/v1, Bearer auth applied globally, and auth endpoints are public")
    void testOpenApiSpecification() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String jsonString = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonString);

        // 1. Verify Global Security Scheme exists in components
        JsonNode securitySchemes = root.path("components").path("securitySchemes");
        assertTrue(securitySchemes.has("Bearer Authentication"), "Security scheme 'Bearer Authentication' should exist in components");
        assertEquals("bearer", securitySchemes.path("Bearer Authentication").path("scheme").asText());
        assertEquals("http", securitySchemes.path("Bearer Authentication").path("type").asText());

        // 2. Verify Global Security Requirement is present on root
        JsonNode globalSecurity = root.path("security");
        assertTrue(globalSecurity.isArray() && globalSecurity.size() > 0, "Global security requirements array must be present");
        assertTrue(globalSecurity.get(0).has("Bearer Authentication"), "Global security must require 'Bearer Authentication'");

        // 3. Verify Paths - Must have single /api/v1 prefix and NO duplicate /api/v1/api/v1
        JsonNode paths = root.path("paths");
        assertTrue(paths.has("/api/v1/tickets"), "Path /api/v1/tickets should exist");
        assertTrue(paths.has("/api/v1/auth/login"), "Path /api/v1/auth/login should exist");
        assertFalse(paths.has("/api/v1/api/v1/tickets"), "Path must NOT contain duplicate /api/v1/api/v1/tickets");
        assertFalse(paths.has("/api/v1/api/v1/auth/login"), "Path must NOT contain duplicate /api/v1/api/v1/auth/login");

        // 4. Verify Public Auth Endpoints have empty security override: security = []
        JsonNode loginOperation = paths.path("/api/v1/auth/login").path("post");
        assertNotNull(loginOperation);
        assertTrue(loginOperation.has("security"), "Login operation should declare security array");
        assertTrue(loginOperation.path("security").isArray() && loginOperation.path("security").isEmpty(),
                "Login operation must have empty security [] so Swagger does not require authentication");

        JsonNode registerOperation = paths.path("/api/v1/auth/register").path("post");
        assertTrue(registerOperation.has("security") && registerOperation.path("security").isEmpty(),
                "Register operation must have empty security []");

        // 5. Verify Protected Endpoints inherit global security (no empty override)
        JsonNode ticketsOperation = paths.path("/api/v1/tickets").path("get");
        assertNotNull(ticketsOperation);
        // Either security is absent on operation (inheriting global security) or has Bearer Authentication
        if (ticketsOperation.has("security")) {
            assertFalse(ticketsOperation.path("security").isEmpty(), "Tickets operation must not override security to empty");
        }
    }
}
