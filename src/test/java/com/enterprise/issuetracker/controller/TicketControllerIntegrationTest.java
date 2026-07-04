package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.TicketRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testCreateTicket_Success() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("System crash on startup")
                .description("The application crashes with a NullPointerException when starting up.")
                .categoryId(1L) // 'Bug' in data-test.sql
                .priorityId(3L) // 'High' in data-test.sql
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticket created successfully"))
                .andExpect(jsonPath("$.data.title").value("System crash on startup"))
                .andExpect(jsonPath("$.data.status.name").value("Open"))
                .andExpect(jsonPath("$.data.reporter.username").value("developer"));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testCreateTicket_ValidationError() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Bug") // Title too short (min 5)
                .description("Short") // Description too short (min 10)
                .categoryId(null) // Required
                .priorityId(null) // Required
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    public void testCreateTicket_Unauthorized() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("System crash on startup")
                .description("The application crashes with a NullPointerException when starting up.")
                .categoryId(1L)
                .priorityId(3L)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testGetTicketById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Ticket not found")));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_Success() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
