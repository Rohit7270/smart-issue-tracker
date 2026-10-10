package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.CommentRequest;
import com.enterprise.issuetracker.dto.TicketRequest;
import com.enterprise.issuetracker.repository.AttachmentRepository;
import com.enterprise.issuetracker.repository.CommentRepository;
import com.enterprise.issuetracker.repository.TicketRepository;
import com.enterprise.issuetracker.service.FileStorageService;
import com.enterprise.issuetracker.service.TicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_NegativePage_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                .param("page", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Page index must not be less than zero")));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_ZeroSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Page size must be between 1 and 100")));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_SizeGreaterThan100_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                .param("page", "0")
                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Page size must be between 1 and 100")));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_InvalidSortDirection_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                .param("sortDir", "banana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort direction")));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_InvalidSortField_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                .param("sortBy", "unsupportedProperty"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort field")));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testSearchTickets_ValidSortingAndPagination_Success() throws Exception {
        // Ensure at least two tickets with distinct titles exist in the result set
        TicketRequest ticketBeta = TicketRequest.builder()
                .title("Beta Ticket Order Verification")
                .description("Verifying ascending alphabetical sorting order.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketBeta)))
                .andExpect(status().isCreated());

        TicketRequest ticketAlpha = TicketRequest.builder()
                .title("Alpha Ticket Order Verification")
                .description("Verifying ascending alphabetical sorting order.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketAlpha)))
                .andExpect(status().isCreated());

        String responseJson = mockMvc.perform(get("/api/v1/tickets")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "title")
                .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable.pageSize").value(5))
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(responseJson).get("data").get("content");
        assertTrue(content.size() >= 2, "Expected at least two tickets in the result set");

        List<String> titles = new ArrayList<>();
        for (JsonNode node : content) {
            titles.add(node.get("title").asText());
        }

        long distinctTitles = titles.stream().distinct().count();
        assertTrue(distinctTitles >= 2, "Expected at least two tickets with distinct titles in result set");

        for (int i = 0; i < titles.size() - 1; i++) {
            String current = titles.get(i);
            String next = titles.get(i + 1);
            assertTrue(current.compareToIgnoreCase(next) <= 0,
                    "Expected titles to be in ascending alphabetical order: '" + current + "' should precede or equal '" + next + "'");
        }
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    public void testAssignTicket_ByManager_Success() throws Exception {
        // 1. Create a ticket
        TicketRequest createRequest = TicketRequest.builder()
                .title("Database connection pool exhaustion")
                .description("HikariCP pool runs out of connections under load.")
                .categoryId(1L)
                .priorityId(3L)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // 2. Manager assigns ticket to developer (id = 3)
        com.enterprise.issuetracker.dto.AssignTicketRequest assignRequest = com.enterprise.issuetracker.dto.AssignTicketRequest.builder()
                .assigneeId(3L)
                .notes("Please investigate Hikari leak")
                .build();

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignee.username").value("developer"))
                .andExpect(jsonPath("$.data.assignments").isArray());

        // 3. Verify assignment history endpoint
        mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].assignedTo.username").value("developer"))
                .andExpect(jsonPath("$.data[0].assignedBy.username").value("manager"));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testAssignTicket_ByUser_Forbidden() throws Exception {
        // Regular USER cannot assign tickets
        com.enterprise.issuetracker.dto.AssignTicketRequest assignRequest = com.enterprise.issuetracker.dto.AssignTicketRequest.builder()
                .assigneeId(3L)
                .notes("Self-assigning")
                .build();

        // Assume ticket ID 1 exists or is created
        TicketRequest createRequest = TicketRequest.builder()
                .title("UI button alignment issue")
                .description("Submit button overlaps footer on mobile devices.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAssignTicket_Unauthenticated_Unauthorized() throws Exception {
        com.enterprise.issuetracker.dto.AssignTicketRequest assignRequest = com.enterprise.issuetracker.dto.AssignTicketRequest.builder()
                .assigneeId(3L)
                .notes("Unauthenticated assignment attempt")
                .build();

        mockMvc.perform(patch("/api/v1/tickets/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testAssignTicket_ByAdmin_Success() throws Exception {
        // 1. Create a ticket
        TicketRequest createRequest = TicketRequest.builder()
                .title("Critical security patch required")
                .description("Upgrade OpenSSL and patch vulnerabilities.")
                .categoryId(1L)
                .priorityId(3L)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // 2. Admin assigns ticket to developer (id = 3)
        com.enterprise.issuetracker.dto.AssignTicketRequest assignRequest = com.enterprise.issuetracker.dto.AssignTicketRequest.builder()
                .assigneeId(3L)
                .notes("Assigned by admin for urgent patch")
                .build();

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignee.username").value("developer"))
                .andExpect(jsonPath("$.data.assignments").isArray());

        // 3. Verify assignment history endpoint
        mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].assignedTo.username").value("developer"))
                .andExpect(jsonPath("$.data[0].assignedBy.username").value("admin"));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testUpdateStatus_ByAssignee_Success() throws Exception {
        // 1. Create ticket assigned to developer (id = 3) via manager
        TicketRequest createRequest = TicketRequest.builder()
                .title("Memory spike during export")
                .description("Heap memory spikes to 95% during large CSV export.")
                .categoryId(1L)
                .priorityId(3L)
                .assigneeId(3L)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .with(user("manager").roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // 2. Developer (assignee) updates status to "In Progress" (id = 2)
        com.enterprise.issuetracker.dto.UpdateTicketStatusRequest statusRequest = com.enterprise.issuetracker.dto.UpdateTicketStatusRequest.builder()
                .statusId(2L)
                .comment("Started profiling memory leak")
                .build();

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status.name").value("In Progress"));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testCreateTicket_ByUser_WithAssignee_Forbidden() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Attempted user assignment")
                .description("Ordinary user trying to assign to developer.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    public void testCreateTicket_ByManager_WithAssignee_Success() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Manager initial assignment")
                .description("Manager creating ticket with developer assignee.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignee.username").value("developer"));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testUpdateTicket_AssigneeModifiesDueDate_Forbidden() throws Exception {
        LocalDate initialDueDate = LocalDate.now().plusDays(5);
        TicketRequest createRequest = TicketRequest.builder()
                .title("Task with deadline")
                .description("Critical bug that must be addressed promptly.")
                .categoryId(1L)
                .priorityId(3L)
                .assigneeId(3L)
                .dueDate(initialDueDate)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .with(user("manager").roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // Assignee developer attempts to modify dueDate
        TicketRequest updateRequest = TicketRequest.builder()
                .title("Task with deadline")
                .description("Critical bug that must be addressed promptly.")
                .categoryId(1L)
                .priorityId(3L)
                .assigneeId(3L)
                .dueDate(initialDueDate.plusDays(10))
                .build();

        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testUpdateTicket_AssigneeClearsDueDate_Forbidden() throws Exception {
        LocalDate initialDueDate = LocalDate.now().plusDays(5);
        TicketRequest createRequest = TicketRequest.builder()
                .title("Task with deadline to clear")
                .description("Critical bug that must be addressed promptly.")
                .categoryId(1L)
                .priorityId(3L)
                .assigneeId(3L)
                .dueDate(initialDueDate)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .with(user("manager").roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // Assignee developer attempts to clear dueDate
        TicketRequest updateRequest = TicketRequest.builder()
                .title("Task with deadline to clear")
                .description("Critical bug that must be addressed promptly.")
                .categoryId(1L)
                .priorityId(3L)
                .assigneeId(3L)
                .dueDate(null)
                .build();

        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testUpdateTicket_ReporterModifiesDueDate_Success() throws Exception {
        LocalDate initialDueDate = LocalDate.now().plusDays(5);
        LocalDate updatedDueDate = LocalDate.now().plusDays(12);

        // Developer creates unassigned ticket
        TicketRequest createRequest = TicketRequest.builder()
                .title("Reporter modifies own due date")
                .description("Developer is reporter and can modify own ticket deadline.")
                .categoryId(1L)
                .priorityId(1L)
                .dueDate(initialDueDate)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        TicketRequest updateRequest = TicketRequest.builder()
                .title("Reporter modifies own due date")
                .description("Developer is reporter and can modify own ticket deadline.")
                .categoryId(1L)
                .priorityId(1L)
                .dueDate(updatedDueDate)
                .build();

        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dueDate").value(updatedDueDate.toString()));
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDeleteTicket_ByReporter_WithAttachmentsAndComments_Success() throws Exception {
        // 1. Reporter creates ticket
        TicketRequest request = TicketRequest.builder()
                .title("Ticket to delete with children")
                .description("Detailed description for ticket that will be deleted with attachments and comments.")
                .categoryId(1L)
                .priorityId(2L)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createRes).get("data").get("id").asLong();

        // 2. Add comment
        CommentRequest commentReq = new CommentRequest();
        commentReq.setContent("Testing cascade deletion of comments");
        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isOk());

        // 3. Upload attachment
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-report.pdf",
                "application/pdf",
                "Sample report pdf content".getBytes()
        );

        String attachRes = mockMvc.perform(multipart("/api/v1/tickets/" + ticketId + "/attachments")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        Long attachmentId = objectMapper.readTree(attachRes).get("data").get("id").asLong();
        String fileUrl = objectMapper.readTree(attachRes).get("data").get("fileUrl").asText();
        String storedFileName = fileStorageService.extractStoredFileName(fileUrl);
        Path physicalPath = fileStorageService.getFileStorageLocation().resolve(storedFileName);

        // Precondition checks: file exists on disk, attachment & comment exist in DB
        assertTrue(Files.exists(physicalPath), "Physical file should exist on disk before deletion");
        assertTrue(attachmentRepository.findById(attachmentId).isPresent(), "Attachment should exist in DB");
        assertFalse(commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).isEmpty(), "Comment should exist in DB");

        // 4. Reporter deletes own ticket
        mockMvc.perform(delete("/api/v1/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticket deleted successfully"));

        // Postcondition checks:
        // Ticket is gone (404)
        mockMvc.perform(get("/api/v1/tickets/" + ticketId))
                .andExpect(status().isNotFound());

        // Child records removed via cascade
        assertFalse(attachmentRepository.findById(attachmentId).isPresent(), "Attachment metadata should be deleted via cascade");
        assertTrue(commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).isEmpty(), "Comments should be deleted via cascade");

        // Physical file deleted from disk
        assertFalse(Files.exists(physicalPath), "Physical uploaded file should be removed from disk");

        // Download endpoint now returns 404
        mockMvc.perform(get("/api/v1/tickets/attachments/download/" + storedFileName))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDeleteTicket_ByAdmin_Success() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Ticket to be deleted by admin")
                .description("Reporter is developer, but admin will delete this ticket.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createRes).get("data").get("id").asLong();

        // Admin deletes ticket
        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticket deleted successfully"));

        mockMvc.perform(get("/api/v1/tickets/" + ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteTicket_ByNonReporterUser_Forbidden() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Ticket owned by manager")
                .description("Regular user developer should not be able to delete manager's ticket.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/tickets")
                .with(user("manager").roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createRes).get("data").get("id").asLong();

        // Non-reporter developer with ROLE_USER attempts deletion
        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                .with(user("developer").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."));

        // Ticket still exists
        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                .with(user("developer").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDeleteTicket_ByNonReporterManager_Forbidden() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Ticket owned by developer")
                .description("A manager who is not the reporter should not be able to delete this ticket.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createRes).get("data").get("id").asLong();

        // Manager attempts deletion
        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                .with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."));

        // Ticket still exists
        mockMvc.perform(get("/api/v1/tickets/" + ticketId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDeleteTicket_NotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Ticket not found")));
    }

    @Test
    public void testDeleteTicket_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDeleteTicket_MissingPhysicalFile_HandledGracefully() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Ticket with missing physical file")
                .description("Upload file, remove it from disk manually, delete ticket succeeds.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createRes).get("data").get("id").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-be-deleted-manually.txt",
                "text/plain",
                "Will be deleted before ticket deletion".getBytes()
        );

        String attachRes = mockMvc.perform(multipart("/api/v1/tickets/" + ticketId + "/attachments")
                .file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long attachmentId = objectMapper.readTree(attachRes).get("data").get("id").asLong();
        String fileUrl = objectMapper.readTree(attachRes).get("data").get("fileUrl").asText();
        String storedFileName = fileStorageService.extractStoredFileName(fileUrl);
        Path physicalPath = fileStorageService.getFileStorageLocation().resolve(storedFileName);

        // Manually delete physical file to simulate missing file on disk
        Files.deleteIfExists(physicalPath);
        assertFalse(Files.exists(physicalPath));

        // Delete ticket should succeed gracefully
        mockMvc.perform(delete("/api/v1/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticket deleted successfully"));

        // Ticket and attachment metadata are removed
        mockMvc.perform(get("/api/v1/tickets/" + ticketId))
                .andExpect(status().isNotFound());
        assertFalse(attachmentRepository.findById(attachmentId).isPresent());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDownloadAttachment_PathTraversal_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/attachments/download/..%2Fsecret.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "developer", roles = "USER")
    public void testDeleteTicket_TransactionRollback_PreservesPhysicalFileAndDatabaseRecords() throws Exception {
        // 1. Create a ticket
        TicketRequest request = TicketRequest.builder()
                .title("Ticket to test transaction rollback")
                .description("Physical file should not be deleted if the database transaction rolls back.")
                .categoryId(1L)
                .priorityId(1L)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(createRes).get("data").get("id").asLong();

        // 2. Upload an attachment
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rollback-test.txt",
                "text/plain",
                "Rollback test file content".getBytes()
        );

        String attachRes = mockMvc.perform(multipart("/api/v1/tickets/" + ticketId + "/attachments")
                .file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long attachmentId = objectMapper.readTree(attachRes).get("data").get("id").asLong();
        String fileUrl = objectMapper.readTree(attachRes).get("data").get("fileUrl").asText();
        String storedFileName = fileStorageService.extractStoredFileName(fileUrl);
        Path physicalPath = fileStorageService.getFileStorageLocation().resolve(storedFileName);

        assertTrue(Files.exists(physicalPath), "Physical file must exist before test transaction");

        // 3. Execute deleteTicket within a transaction that forces rollback
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            ticketService.deleteTicket(ticketId, "developer");
            // Force transaction rollback
            status.setRollbackOnly();
            return null;
        });

        // 4. Verify postconditions after transaction rollback:
        // Physical file must STILL EXIST on disk!
        assertTrue(Files.exists(physicalPath), "Physical file must NOT be deleted when database transaction rolls back");
        // Database records must STILL EXIST!
        assertTrue(ticketRepository.findById(ticketId).isPresent(), "Ticket must remain in database after transaction rollback");
        assertTrue(attachmentRepository.findById(attachmentId).isPresent(), "Attachment must remain in database after transaction rollback");

        // Cleanup: Now delete the ticket with commit
        txTemplate.execute(status -> {
            ticketService.deleteTicket(ticketId, "developer");
            return null;
        });
        assertFalse(Files.exists(physicalPath), "Physical file must be deleted after transaction commits");
        assertFalse(ticketRepository.findById(ticketId).isPresent());
    }
}
