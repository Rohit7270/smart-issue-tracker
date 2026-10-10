package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.AssignTicketRequest;
import com.enterprise.issuetracker.dto.TicketRequest;
import com.enterprise.issuetracker.dto.TicketResponse;
import com.enterprise.issuetracker.dto.UpdateTicketStatusRequest;
import com.enterprise.issuetracker.entity.*;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Isolated Service Layer Unit Test using Mockito.
 * Tests business workflow rules, status transition validation, role permissions, and repository interactions.
 */
@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PriorityRepository priorityRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TicketService ticketService;

    private User reporterUser;
    private User managerUser;
    private User developerUser;
    private User adminUser;
    private Category bugCategory;
    private Priority highPriority;
    private Status openStatus;
    private Status inProgressStatus;
    private Status closedStatus;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().id(1L).name(RoleType.ROLE_USER).build();
        Role managerRole = Role.builder().id(2L).name(RoleType.ROLE_MANAGER).build();
        Role adminRole = Role.builder().id(3L).name(RoleType.ROLE_ADMIN).build();

        reporterUser = User.builder()
                .id(1L)
                .username("reporter")
                .email("reporter@tracker.com")
                .fullName("Reporter User")
                .active(true)
                .roles(Set.of(userRole))
                .build();

        managerUser = User.builder()
                .id(2L)
                .username("manager")
                .email("manager@tracker.com")
                .fullName("Project Manager")
                .active(true)
                .roles(Set.of(managerRole))
                .build();

        developerUser = User.builder()
                .id(3L)
                .username("developer")
                .email("developer@tracker.com")
                .fullName("Developer")
                .active(true)
                .roles(Set.of(userRole))
                .build();

        adminUser = User.builder()
                .id(4L)
                .username("admin")
                .email("admin@tracker.com")
                .fullName("Administrator")
                .active(true)
                .roles(Set.of(adminRole))
                .build();

        bugCategory = Category.builder().id(1L).name("Bug").description("System bug").build();
        highPriority = Priority.builder().id(1L).name("High").weight(3).build();

        openStatus = Status.builder().id(1L).name("Open").closedStatus(false).build();
        inProgressStatus = Status.builder().id(2L).name("In Progress").closedStatus(false).build();
        closedStatus = Status.builder().id(4L).name("Closed").closedStatus(true).build();
    }

    @Test
    @DisplayName("Create Ticket - Success with default Open status")
    void createTicket_Success() {
        TicketRequest request = TicketRequest.builder()
                .title("NullPointer in login controller")
                .description("Application crashes when user submits null credentials")
                .categoryId(1L)
                .priorityId(1L)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(bugCategory));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(highPriority));
        when(statusRepository.findByName("Open")).thenReturn(Optional.of(openStatus));

        Ticket mockSavedTicket = Ticket.builder()
                .id(100L)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(bugCategory)
                .priority(highPriority)
                .status(openStatus)
                .reporter(reporterUser)
                .build();

        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockSavedTicket);

        TicketResponse response = ticketService.createTicket(request, "reporter");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("NullPointer in login controller", response.getTitle());
        assertEquals("Open", response.getStatus().getName());
        assertEquals("reporter", response.getReporter().getUsername());

        verify(activityLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Update Ticket Status - Valid transition Open to In Progress")
    void updateTicketStatus_ValidTransition_Success() {
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Memory leak")
                .description("Heap memory leaks under heavy load")
                .status(openStatus)
                .assignee(developerUser)
                .reporter(reporterUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        UpdateTicketStatusRequest statusRequest = UpdateTicketStatusRequest.builder()
                .statusId(2L)
                .comment("Started debugging heap dump")
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(inProgressStatus));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicketStatus(100L, statusRequest, "developer");

        assertNotNull(response);
        assertEquals("In Progress", response.getStatus().getName());

        verify(commentRepository, times(1)).save(any());
        verify(activityLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Update Ticket Status - Invalid transition Closed to In Progress throws BadRequestException")
    void updateTicketStatus_InvalidTransition_ThrowsBadRequest() {
        Ticket closedTicket = Ticket.builder()
                .id(100L)
                .title("Resolved issue")
                .description("Previously resolved and closed issue")
                .status(closedStatus)
                .assignee(developerUser)
                .reporter(reporterUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        UpdateTicketStatusRequest statusRequest = UpdateTicketStatusRequest.builder()
                .statusId(2L)
                .comment("Trying to move closed directly to In Progress")
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(closedTicket));
        when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(inProgressStatus));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                ticketService.updateTicketStatus(100L, statusRequest, "developer")
        );

        assertTrue(exception.getMessage().contains("A closed ticket must be reopened to 'Open' status first"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Ticket Status - Reporter is allowed to update status of their own unassigned ticket")
    void updateTicketStatus_ByReporter_Unassigned_Success() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Database connection issue")
                .description("Connection pool timeout")
                .status(openStatus)
                .assignee(null)
                .reporter(reporterUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        UpdateTicketStatusRequest statusRequest = UpdateTicketStatusRequest.builder()
                .statusId(2L)
                .comment("Moving ticket to In Progress")
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(inProgressStatus));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicketStatus(100L, statusRequest, "reporter");

        assertNotNull(response);
        assertEquals("In Progress", response.getStatus().getName());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Update Ticket Status - Access Denied for unrelated ordinary user who is neither reporter nor assignee")
    void updateTicketStatus_AccessDenied_UnauthorizedUser_ThrowsException() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Database connection issue")
                .description("Connection pool timeout")
                .status(openStatus)
                .assignee(developerUser)
                .reporter(managerUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        UpdateTicketStatusRequest statusRequest = UpdateTicketStatusRequest.builder()
                .statusId(2L)
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser)); // reporterUser is neither assignee (developerUser), reporter (managerUser), nor manager/admin

        assertThrows(AccessDeniedException.class, () ->
                ticketService.updateTicketStatus(100L, statusRequest, "reporter")
        );
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign Ticket - Manager assigns ticket to developer")
    void assignTicket_ByManager_Success() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Fix login performance")
                .description("Sluggish queries on user login")
                .status(openStatus)
                .reporter(reporterUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        AssignTicketRequest assignRequest = AssignTicketRequest.builder()
                .assigneeId(3L)
                .notes("Assigning to senior engineer")
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(managerUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(developerUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.assignTicket(100L, assignRequest, "manager");

        assertNotNull(response);
        assertEquals("developer", response.getAssignee().getUsername());

        verify(assignmentRepository, times(1)).save(any());
        verify(activityLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Assign Ticket - Regular user attempting to assign throws AccessDeniedException")
    void assignTicket_ByUser_ThrowsAccessDeniedException() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Fix login performance")
                .description("Sluggish queries on user login")
                .status(openStatus)
                .reporter(reporterUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        AssignTicketRequest assignRequest = AssignTicketRequest.builder()
                .assigneeId(3L)
                .notes("Self-assigning or delegating")
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.assignTicket(100L, assignRequest, "reporter")
        );

        assertTrue(exception.getMessage().contains("Only Admins and Managers have permission to assign tickets"));
        verify(ticketRepository, never()).save(any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get Ticket By ID - Not Found throws ResourceNotFoundException")
    void getTicketById_NotFound_ThrowsException() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicketById(999L));
    }

    @Test
    @DisplayName("Update Ticket - Regular user attempting to unassign throws AccessDeniedException")
    void updateTicket_ReporterAttemptsToUnassign_ThrowsAccessDeniedException() {
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Database slow query")
                .description("Query takes over 10 seconds to respond.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Database slow query")
                .description("Query takes over 10 seconds to respond.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(null) // Attempting to unassign assigned ticket
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.updateTicket(100L, request, "reporter")
        );

        assertTrue(exception.getMessage().contains("Regular users cannot assign or unassign tickets"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Ticket - Regular user attempting to assign unassigned ticket throws AccessDeniedException")
    void updateTicket_ReporterAttemptsToAssign_ThrowsAccessDeniedException() {
        Ticket unassignedTicket = Ticket.builder()
                .id(100L)
                .title("UI layout issue")
                .description("Alignment issue on Safari browser.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(null) // Currently unassigned
                .category(bugCategory)
                .priority(highPriority)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("UI layout issue")
                .description("Alignment issue on Safari browser.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L) // Attempting to assign
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(unassignedTicket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.updateTicket(100L, request, "reporter")
        );

        assertTrue(exception.getMessage().contains("Regular users cannot assign or unassign tickets"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Ticket - Invalid status transition through general update endpoint throws BadRequestException")
    void updateTicket_InvalidStatusTransition_ThrowsBadRequestException() {
        Ticket closedTicket = Ticket.builder()
                .id(100L)
                .title("Resolved issue")
                .description("Ticket previously completed and closed.")
                .status(closedStatus) // Currently 'Closed'
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Resolved issue")
                .description("Ticket previously completed and closed.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .statusId(2L) // Attempting direct jump to 'In Progress' without reopening to 'Open'
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(closedTicket));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(managerUser));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(inProgressStatus));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                ticketService.updateTicket(100L, request, "manager")
        );

        assertTrue(exception.getMessage().contains("A closed ticket must be reopened to 'Open' status first"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Ticket - Regular reporter updates title and description while keeping assignee succeeds")
    void updateTicket_ReporterUpdatesDetails_PreservesAssignee_Success() {
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Old Title for Bug")
                .description("Old description with minimal details.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("New Clarified Title")
                .description("New updated and detailed description.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L) // Preserves current developer assignee
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicket(100L, request, "reporter");

        assertNotNull(response);
        assertEquals("New Clarified Title", response.getTitle());
        assertEquals("developer", response.getAssignee().getUsername());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Update Ticket - Manager reassigns ticket to new user succeeds")
    void updateTicket_ManagerReassigns_Success() {
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Server high latency")
                .description("High response time during peak hours.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser) // Initially developerUser (id=3)
                .category(bugCategory)
                .priority(highPriority)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Server high latency")
                .description("High response time during peak hours.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(1L) // Manager reassigns to reporterUser (id=1)
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(managerUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporterUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicket(100L, request, "manager");

        assertNotNull(response);
        assertEquals("reporter", response.getAssignee().getUsername());
        verify(assignmentRepository, times(1)).save(any());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Create Ticket - Regular user attempting to set assignee throws AccessDeniedException")
    void createTicket_ByUser_WithAssignee_ThrowsAccessDeniedException() {
        TicketRequest request = TicketRequest.builder()
                .title("UI layout issue")
                .description("Alignment issue on Safari browser.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L) // Regular user tries to assign to developer
                .build();

        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(bugCategory));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(highPriority));
        when(statusRepository.findByName("Open")).thenReturn(Optional.of(openStatus));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.createTicket(request, "reporter")
        );

        assertTrue(exception.getMessage().contains("Only Admins and Managers have permission to assign tickets"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Ticket - Manager sets initial assignee successfully")
    void createTicket_ByManager_WithAssignee_Success() {
        TicketRequest request = TicketRequest.builder()
                .title("Database slow query")
                .description("Query takes over 10 seconds to respond.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L) // Manager assigns to developer
                .build();

        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(managerUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(bugCategory));
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(highPriority));
        when(statusRepository.findByName("Open")).thenReturn(Optional.of(openStatus));
        when(userRepository.findById(3L)).thenReturn(Optional.of(developerUser));

        Ticket mockSavedTicket = Ticket.builder()
                .id(101L)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(bugCategory)
                .priority(highPriority)
                .status(openStatus)
                .reporter(managerUser)
                .assignee(developerUser)
                .build();

        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockSavedTicket);

        TicketResponse response = ticketService.createTicket(request, "manager");

        assertNotNull(response);
        assertEquals("developer", response.getAssignee().getUsername());
        verify(assignmentRepository, times(1)).save(any());
        verify(ticketRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Update Ticket - Assignee attempting to modify due date throws AccessDeniedException")
    void updateTicket_AssigneeModifiesDueDate_ThrowsAccessDeniedException() {
        LocalDate originalDueDate = LocalDate.now().plusDays(5);
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .dueDate(originalDueDate)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .dueDate(originalDueDate.plusDays(10)) // Assignee tries to extend due date
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.updateTicket(100L, request, "developer")
        );

        assertTrue(exception.getMessage().contains("Assignees can only update the status of this ticket"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Ticket - Assignee attempting to clear due date throws AccessDeniedException")
    void updateTicket_AssigneeClearsDueDate_ThrowsAccessDeniedException() {
        LocalDate originalDueDate = LocalDate.now().plusDays(5);
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .dueDate(originalDueDate)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .dueDate(null) // Assignee tries to wipe due date
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.updateTicket(100L, request, "developer")
        );

        assertTrue(exception.getMessage().contains("Assignees can only update the status of this ticket"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Ticket - Assignee preserves existing due date and updates status successfully")
    void updateTicket_AssigneePreservesDueDate_Success() {
        LocalDate originalDueDate = LocalDate.now().plusDays(5);
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .dueDate(originalDueDate)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .dueDate(originalDueDate) // Exactly preserves existing due date
                .statusId(2L) // Updates status to In Progress
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(inProgressStatus));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicket(100L, request, "developer");

        assertNotNull(response);
        assertEquals("In Progress", response.getStatus().getName());
        assertEquals(originalDueDate, response.getDueDate());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Update Ticket - Reporter modifies due date successfully")
    void updateTicket_ReporterModifiesDueDate_Success() {
        LocalDate originalDueDate = LocalDate.now().plusDays(5);
        LocalDate newDueDate = LocalDate.now().plusDays(15);
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .dueDate(originalDueDate)
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .dueDate(newDueDate) // Reporter can modify due date
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicket(100L, request, "reporter");

        assertNotNull(response);
        assertEquals(newDueDate, response.getDueDate());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Update Ticket - Manager modifies due date successfully")
    void updateTicket_ManagerModifiesDueDate_Success() {
        LocalDate newDueDate = LocalDate.now().plusDays(20);
        Ticket existingTicket = Ticket.builder()
                .id(100L)
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .status(openStatus)
                .reporter(reporterUser)
                .assignee(developerUser)
                .category(bugCategory)
                .priority(highPriority)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        TicketRequest request = TicketRequest.builder()
                .title("Memory leak in payment gateway")
                .description("OOM error after continuous transactions.")
                .categoryId(1L)
                .priorityId(1L)
                .assigneeId(3L)
                .dueDate(newDueDate) // Manager can modify due date
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(managerUser));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicket(100L, request, "manager");

        assertNotNull(response);
        assertEquals(newDueDate, response.getDueDate());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Delete Ticket - Reporter deletes own ticket successfully and cleans up physical files")
    void deleteTicket_ByReporter_Success() {
        Attachment attachment = Attachment.builder()
                .id(10L)
                .fileName("report.pdf")
                .fileType("application/pdf")
                .fileUrl("/api/v1/tickets/attachments/download/stored-uuid.pdf")
                .build();

        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Issue to delete")
                .reporter(reporterUser)
                .attachments(new ArrayList<>(List.of(attachment)))
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));

        ticketService.deleteTicket(100L, "reporter");

        verify(ticketRepository, times(1)).delete(ticket);
        verify(activityLogRepository, times(1)).save(any());
        verify(fileStorageService, times(1)).deleteAttachmentFile("/api/v1/tickets/attachments/download/stored-uuid.pdf");
    }

    @Test
    @DisplayName("Delete Ticket - Admin deletes another user's ticket successfully")
    void deleteTicket_ByAdmin_Success() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Issue to delete")
                .reporter(reporterUser)
                .attachments(new ArrayList<>())
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        ticketService.deleteTicket(100L, "admin");

        verify(ticketRepository, times(1)).delete(ticket);
        verify(activityLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Delete Ticket - Non-reporter user receives AccessDeniedException")
    void deleteTicket_ByNonReporterUser_ThrowsAccessDeniedException() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Issue to delete")
                .reporter(reporterUser)
                .attachments(new ArrayList<>())
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.deleteTicket(100L, "developer")
        );

        assertTrue(exception.getMessage().contains("Only the reporter or an Admin can delete this ticket"));
        verify(ticketRepository, never()).delete(any(Ticket.class));
        verify(fileStorageService, never()).deleteAttachmentFile(any());
    }

    @Test
    @DisplayName("Delete Ticket - Non-reporter manager receives AccessDeniedException")
    void deleteTicket_ByNonReporterManager_ThrowsAccessDeniedException() {
        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Issue to delete")
                .reporter(reporterUser)
                .attachments(new ArrayList<>())
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(managerUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                ticketService.deleteTicket(100L, "manager")
        );

        assertTrue(exception.getMessage().contains("Only the reporter or an Admin can delete this ticket"));
        verify(ticketRepository, never()).delete(any(Ticket.class));
        verify(fileStorageService, never()).deleteAttachmentFile(any());
    }

    @Test
    @DisplayName("Delete Ticket - Non-existent ticket throws ResourceNotFoundException")
    void deleteTicket_NotFound_ThrowsResourceNotFoundException() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                ticketService.deleteTicket(999L, "reporter")
        );
        verify(ticketRepository, never()).delete(any(Ticket.class));
    }

    @Test
    @DisplayName("Delete Ticket - Missing physical file is handled gracefully without failing deletion")
    void deleteTicket_MissingPhysicalFile_HandledGracefully() {
        Attachment attachment = Attachment.builder()
                .id(10L)
                .fileName("missing.pdf")
                .fileType("application/pdf")
                .fileUrl("/api/v1/tickets/attachments/download/missing.pdf")
                .build();

        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Ticket with missing file")
                .reporter(reporterUser)
                .attachments(new ArrayList<>(List.of(attachment)))
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));
        when(fileStorageService.deleteAttachmentFile(anyString())).thenReturn(false); // file missing on disk

        assertDoesNotThrow(() -> ticketService.deleteTicket(100L, "reporter"));

        verify(ticketRepository, times(1)).delete(ticket);
        verify(fileStorageService, times(1)).deleteAttachmentFile("/api/v1/tickets/attachments/download/missing.pdf");
    }

    @Test
    @DisplayName("Delete Ticket - In active transaction, physical files are only deleted after commit")
    void deleteTicket_InActiveTransaction_DeletesFilesOnlyAfterCommit() {
        Attachment attachment = Attachment.builder()
                .id(10L)
                .fileName("report.pdf")
                .fileType("application/pdf")
                .fileUrl("/api/v1/tickets/attachments/download/stored-uuid.pdf")
                .build();

        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Issue with attachment")
                .reporter(reporterUser)
                .attachments(new ArrayList<>(List.of(attachment)))
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            ticketService.deleteTicket(100L, "reporter");

            // Verify DB delete occurred but physical file cleanup was NOT called yet before commit
            verify(ticketRepository, times(1)).delete(ticket);
            verify(fileStorageService, never()).deleteAttachmentFile(any());

            // Simulate transaction commit
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            // Verify physical file was deleted after commit
            verify(fileStorageService, times(1)).deleteAttachmentFile("/api/v1/tickets/attachments/download/stored-uuid.pdf");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    @DisplayName("Delete Ticket - In active transaction, physical files are NOT deleted if transaction rolls back")
    void deleteTicket_InActiveTransaction_DoesNotDeleteFilesOnRollback() {
        Attachment attachment = Attachment.builder()
                .id(10L)
                .fileName("report.pdf")
                .fileType("application/pdf")
                .fileUrl("/api/v1/tickets/attachments/download/stored-uuid.pdf")
                .build();

        Ticket ticket = Ticket.builder()
                .id(100L)
                .title("Issue with attachment")
                .reporter(reporterUser)
                .attachments(new ArrayList<>(List.of(attachment)))
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporterUser));

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            ticketService.deleteTicket(100L, "reporter");

            // Verify file cleanup was NOT called during deleteTicket
            verify(fileStorageService, never()).deleteAttachmentFile(any());

            // Simulate transaction rollback (afterCompletion with STATUS_ROLLED_BACK)
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            // Verify physical file was NEVER deleted
            verify(fileStorageService, never()).deleteAttachmentFile(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
