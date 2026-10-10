package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.ActivityLogResponse;
import com.enterprise.issuetracker.dto.AssignTicketRequest;
import com.enterprise.issuetracker.dto.AssignmentResponse;
import com.enterprise.issuetracker.dto.AttachmentResponse;
import com.enterprise.issuetracker.dto.CommentRequest;
import com.enterprise.issuetracker.dto.CommentResponse;
import com.enterprise.issuetracker.dto.TicketRequest;
import com.enterprise.issuetracker.dto.TicketResponse;
import com.enterprise.issuetracker.dto.UpdateTicketStatusRequest;
import com.enterprise.issuetracker.entity.ActivityLog;
import com.enterprise.issuetracker.entity.Assignment;
import com.enterprise.issuetracker.entity.Attachment;
import com.enterprise.issuetracker.entity.Category;
import com.enterprise.issuetracker.entity.Comment;
import com.enterprise.issuetracker.entity.Priority;
import com.enterprise.issuetracker.entity.RoleType;
import com.enterprise.issuetracker.entity.Status;
import com.enterprise.issuetracker.entity.Ticket;
import com.enterprise.issuetracker.entity.User;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.mapper.ActivityLogMapper;
import com.enterprise.issuetracker.mapper.AssignmentMapper;
import com.enterprise.issuetracker.mapper.AttachmentMapper;
import com.enterprise.issuetracker.mapper.CommentMapper;
import com.enterprise.issuetracker.mapper.TicketMapper;
import com.enterprise.issuetracker.repository.ActivityLogRepository;
import com.enterprise.issuetracker.repository.AssignmentRepository;
import com.enterprise.issuetracker.repository.AttachmentRepository;
import com.enterprise.issuetracker.repository.CategoryRepository;
import com.enterprise.issuetracker.repository.CommentRepository;
import com.enterprise.issuetracker.repository.PriorityRepository;
import com.enterprise.issuetracker.repository.StatusRepository;
import com.enterprise.issuetracker.repository.TicketRepository;
import com.enterprise.issuetracker.repository.TicketSpecification;
import com.enterprise.issuetracker.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TicketService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "createdAt", "updatedAt", "dueDate");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AssignmentRepository assignmentRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            PriorityRepository priorityRepository,
            StatusRepository statusRepository,
            CommentRepository commentRepository,
            AttachmentRepository attachmentRepository,
            ActivityLogRepository activityLogRepository,
            AssignmentRepository assignmentRepository,
            FileStorageService fileStorageService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.activityLogRepository = activityLogRepository;
        this.assignmentRepository = assignmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public TicketResponse createTicket(TicketRequest request, String username) {
        log.info("User '{}' is creating a new ticket: {}", username, request.getTitle());
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Priority priority = priorityRepository.findById(request.getPriorityId())
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found"));

        // Defaults to 'Open' status
        Status status = statusRepository.findByName("Open")
                .orElseThrow(() -> new ResourceNotFoundException("Default status 'Open' not configured in database."));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            boolean isAdmin = reporter.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
            boolean isManager = reporter.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_MANAGER);
            if (!isAdmin && !isManager) {
                throw new AccessDeniedException("Only Admins and Managers have permission to assign tickets.");
            }
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        Ticket ticket = TicketMapper.toEntity(request, category, priority, status, reporter, assignee);

        Ticket savedTicket = ticketRepository.save(ticket);

        // Persistent Assignment Management
        if (assignee != null) {
            Assignment initialAssignment = Assignment.builder()
                    .ticket(savedTicket)
                    .assignedTo(assignee)
                    .assignedBy(reporter)
                    .notes("Initial assignment on ticket creation")
                    .build();
            assignmentRepository.save(initialAssignment);
            savedTicket.getAssignments().add(initialAssignment);
        }

        // Audit Trail entry
        activityLogRepository.save(ActivityLog.builder()
                .ticketId(savedTicket.getId())
                .user(reporter)
                .action("Created ticket: '" + ticket.getTitle() + "'")
                .build());

        return TicketMapper.toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest request, String username) {
        log.info("User '{}' is updating ticket ID: {}", username, id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
        boolean isManager = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_MANAGER);
        boolean isReporter = ticket.getReporter().getId().equals(actor.getId());
        boolean isAssignee = ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actor.getId());

        if (!isAdmin && !isManager && !isReporter && !isAssignee) {
            throw new AccessDeniedException("You do not have permission to modify this ticket.");
        }

        // Action validation checks for normal User roles
        if (!isAdmin && !isManager) {
            Long currentAssigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;

            if (isReporter) {
                // Reporter wants to edit. They can only do so if the ticket is NOT closed
                if (ticket.getStatus().isClosedStatus()) {
                    throw new BadRequestException("Cannot update details of a resolved or closed ticket.");
                }
                // Reporter cannot reassign, assign, unassign, or change priority
                if (!Objects.equals(ticket.getPriority().getId(), request.getPriorityId()) ||
                        !Objects.equals(currentAssigneeId, request.getAssigneeId())) {
                    throw new AccessDeniedException("Regular users cannot assign or unassign tickets, or modify priority levels.");
                }
            } else if (isAssignee) {
                // Assignee (who is NOT reporter) can ONLY change the status of the ticket
                if (!Objects.equals(ticket.getTitle(), request.getTitle()) ||
                        !Objects.equals(ticket.getDescription(), request.getDescription()) ||
                        !Objects.equals(ticket.getCategory().getId(), request.getCategoryId()) ||
                        !Objects.equals(ticket.getPriority().getId(), request.getPriorityId()) ||
                        !Objects.equals(ticket.getDueDate(), request.getDueDate()) ||
                        !Objects.equals(currentAssigneeId, request.getAssigneeId())) {
                    throw new AccessDeniedException("Assignees can only update the status of this ticket.");
                }
            }
        }

        List<String> auditLogs = new ArrayList<>();

        // 1. Process Category
        if (!ticket.getCategory().getId().equals(request.getCategoryId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            auditLogs.add("Category changed from '" + ticket.getCategory().getName() + "' to '" + newCategory.getName() + "'");
            ticket.setCategory(newCategory);
        }

        // 2. Process Priority
        if (!ticket.getPriority().getId().equals(request.getPriorityId())) {
            Priority newPriority = priorityRepository.findById(request.getPriorityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Priority not found"));
            auditLogs.add("Priority changed from '" + ticket.getPriority().getName() + "' to '" + newPriority.getName() + "'");
            ticket.setPriority(newPriority);
        }

        // 3. Process Status
        if (request.getStatusId() != null && !ticket.getStatus().getId().equals(request.getStatusId())) {
            Status newStatus = statusRepository.findById(request.getStatusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found"));
            validateStatusTransition(ticket.getStatus(), newStatus);
            auditLogs.add("Status changed from '" + ticket.getStatus().getName() + "' to '" + newStatus.getName() + "'");
            ticket.setStatus(newStatus);
        }

        // 4. Process Assignee
        if (request.getAssigneeId() == null) {
            if (ticket.getAssignee() != null) {
                auditLogs.add("Removed assignee (was '" + ticket.getAssignee().getUsername() + "')");
                ticket.setAssignee(null);
            }
        } else if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(request.getAssigneeId())) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found"));
            String oldAssigneeName = ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : "Unassigned";
            auditLogs.add("Assignee changed from '" + oldAssigneeName + "' to '" + newAssignee.getUsername() + "'");
            ticket.setAssignee(newAssignee);

            // Record persistent assignment
            Assignment assignment = Assignment.builder()
                    .ticket(ticket)
                    .assignedTo(newAssignee)
                    .assignedBy(actor)
                    .notes("Reassigned via ticket update")
                    .build();
            assignmentRepository.save(assignment);
            ticket.getAssignments().add(assignment);
        }

        // 5. Title & Description
        if (!ticket.getTitle().equals(request.getTitle())) {
            ticket.setTitle(request.getTitle());
        }
        if (!ticket.getDescription().equals(request.getDescription())) {
            ticket.setDescription(request.getDescription());
        }
        ticket.setDueDate(request.getDueDate());

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Save activity audit trails
        for (String actionMsg : auditLogs) {
            activityLogRepository.save(ActivityLog.builder()
                    .ticketId(updatedTicket.getId())
                    .user(actor)
                    .action(actionMsg)
                    .build());
        }

        return TicketMapper.toResponse(updatedTicket);
    }

    @Transactional
    public void deleteTicket(Long id, String username) {
        log.info("User '{}' is deleting ticket ID: {}", username, id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
        boolean isReporter = ticket.getReporter().getId().equals(actor.getId());

        if (!isAdmin && !isReporter) {
            throw new AccessDeniedException("Only the reporter or an Admin can delete this ticket.");
        }

        // Collect physical attachment file URLs before database cascade deletion removes them
        List<String> fileUrls = (ticket.getAttachments() != null)
                ? ticket.getAttachments().stream()
                        .map(Attachment::getFileUrl)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        ticketRepository.delete(ticket);

        activityLogRepository.save(ActivityLog.builder()
                .user(actor)
                .action("Deleted ticket ID #" + id + ": '" + ticket.getTitle() + "'")
                .build());

        // Clean up physical attachment files on filesystem after database transaction commits
        if (fileStorageService != null && !fileUrls.isEmpty()) {
            Runnable fileCleanupTask = () -> {
                for (String fileUrl : fileUrls) {
                    try {
                        fileStorageService.deleteAttachmentFile(fileUrl);
                    } catch (Exception ex) {
                        log.error("Partial failure: Could not delete physical attachment file for URL '{}': {}", fileUrl, ex.getMessage(), ex);
                    }
                }
            };

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileCleanupTask.run();
                    }
                });
            } else {
                fileCleanupTask.run();
            }
        }
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        log.info("Fetching ticket by ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + id));
        return TicketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> searchTickets(
            String keyword,
            Long categoryId,
            Long priorityId,
            Long statusId,
            Long reporterId,
            Long assigneeId,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Searching tickets with specifications");

        if (page < 0) {
            throw new BadRequestException("Page index must not be less than zero.");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100.");
        }
        if (sortDir == null || (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir))) {
            throw new BadRequestException("Invalid sort direction '" + sortDir + "'. Must be 'asc' or 'desc'.");
        }
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field '" + sortBy + "'. Allowed fields are: " + String.join(", ", ALLOWED_SORT_FIELDS));
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.filterTickets(keyword, categoryId, priorityId, statusId, reporterId, assigneeId),
                pageable
        );

        return tickets.map(TicketMapper::toResponse);
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request, String username) {
        log.info("User '{}' is adding a comment to ticket ID: {}", username, ticketId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Comment comment = Comment.builder()
                .ticket(ticket)
                .user(user)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);

        activityLogRepository.save(ActivityLog.builder()
                .ticketId(ticketId)
                .user(user)
                .action("Added comment: '" + (comment.getContent().length() > 30 ? comment.getContent().substring(0, 27) + "..." : comment.getContent()) + "'")
                .build());

        return CommentMapper.toResponse(savedComment);
    }

    @Transactional
    public AttachmentResponse addAttachment(Long ticketId, String fileName, String fileType, String fileUrl, String username) {
        log.info("User '{}' is uploading file '{}' to ticket ID: {}", username, fileName, ticketId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .fileName(fileName)
                .fileType(fileType)
                .fileUrl(fileUrl)
                .build();

        Attachment savedAttachment = attachmentRepository.save(attachment);

        activityLogRepository.save(ActivityLog.builder()
                .ticketId(ticketId)
                .user(user)
                .action("Uploaded attachment: '" + fileName + "'")
                .build());

        return AttachmentMapper.toResponse(savedAttachment);
    }

    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request, String username) {
        log.info("User '{}' is assigning ticket ID: {} to user ID: {}", username, id, request.getAssigneeId());
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
        boolean isManager = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_MANAGER);

        if (!isAdmin && !isManager) {
            throw new AccessDeniedException("Only Admins and Managers have permission to assign tickets.");
        }

        User newAssignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found with ID: " + request.getAssigneeId()));

        String oldAssigneeName = ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : "Unassigned";
        ticket.setAssignee(newAssignee);
        Ticket savedTicket = ticketRepository.save(ticket);

        String note = (request.getNotes() != null && !request.getNotes().isBlank())
                ? request.getNotes()
                : "Assigned by " + actor.getUsername();

        Assignment assignment = Assignment.builder()
                .ticket(savedTicket)
                .assignedTo(newAssignee)
                .assignedBy(actor)
                .notes(note)
                .build();
        assignmentRepository.save(assignment);
        savedTicket.getAssignments().add(assignment);

        activityLogRepository.save(ActivityLog.builder()
                .ticketId(savedTicket.getId())
                .user(actor)
                .action("Assigned ticket from '" + oldAssigneeName + "' to '" + newAssignee.getUsername() + "'")
                .build());

        return TicketMapper.toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long id, UpdateTicketStatusRequest request, String username) {
        log.info("User '{}' is updating status of ticket ID: {} to status ID: {}", username, id, request.getStatusId());
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
        boolean isManager = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_MANAGER);
        boolean isReporter = ticket.getReporter() != null && ticket.getReporter().getId().equals(actor.getId());
        boolean isAssignee = ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actor.getId());

        if (!isAdmin && !isManager && !isAssignee && !isReporter) {
            throw new AccessDeniedException("Only the assigned user, ticket reporter, a Manager, or an Admin can change the ticket status.");
        }

        Status newStatus = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with ID: " + request.getStatusId()));

        validateStatusTransition(ticket.getStatus(), newStatus);

        String oldStatusName = ticket.getStatus().getName();
        ticket.setStatus(newStatus);
        Ticket savedTicket = ticketRepository.save(ticket);

        activityLogRepository.save(ActivityLog.builder()
                .ticketId(savedTicket.getId())
                .user(actor)
                .action("Status updated from '" + oldStatusName + "' to '" + newStatus.getName() + "'")
                .build());

        if (request.getComment() != null && !request.getComment().isBlank()) {
            Comment comment = Comment.builder()
                    .ticket(savedTicket)
                    .user(actor)
                    .content("Status update (" + newStatus.getName() + "): " + request.getComment())
                    .build();
            commentRepository.save(comment);
            savedTicket.getComments().add(comment);
        }

        return TicketMapper.toResponse(savedTicket);
    }

    /**
     * Validates state transitions in the ticket lifecycle workflow.
     * Prevents invalid state jumps (e.g., from 'Closed' directly to 'In Progress' without reopening to 'Open').
     */
    private void validateStatusTransition(Status currentStatus, Status newStatus) {
        if (currentStatus.getId().equals(newStatus.getId())) {
            return;
        }

        String currentName = currentStatus.getName();
        String newName = newStatus.getName();

        if ("Closed".equalsIgnoreCase(currentName) && !"Open".equalsIgnoreCase(newName)) {
            throw new BadRequestException(
                    String.format("Invalid status transition from '%s' to '%s'. A closed ticket must be reopened to 'Open' status first.", currentName, newName)
            );
        }
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getTicketAssignments(Long ticketId) {
        log.info("Fetching assignment history for ticket ID: {}", ticketId);
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with ID: " + ticketId);
        }
        return assignmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
                .map(AssignmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getAllActivityLogs(int page, int size) {
        log.info("Fetching activity logs list");
        Pageable pageable = PageRequest.of(page, size);
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ActivityLogMapper::toResponse);
    }
}
