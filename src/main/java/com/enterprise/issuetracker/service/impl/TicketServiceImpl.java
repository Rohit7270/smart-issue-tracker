package com.enterprise.issuetracker.service.impl;

import com.enterprise.issuetracker.dto.ActivityLogResponse;
import com.enterprise.issuetracker.dto.AttachmentResponse;
import com.enterprise.issuetracker.dto.CommentRequest;
import com.enterprise.issuetracker.dto.CommentResponse;
import com.enterprise.issuetracker.dto.TicketRequest;
import com.enterprise.issuetracker.dto.TicketResponse;
import com.enterprise.issuetracker.entity.ActivityLog;
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
import com.enterprise.issuetracker.mapper.AttachmentMapper;
import com.enterprise.issuetracker.mapper.CommentMapper;
import com.enterprise.issuetracker.mapper.TicketMapper;
import com.enterprise.issuetracker.repository.ActivityLogRepository;
import com.enterprise.issuetracker.repository.AttachmentRepository;
import com.enterprise.issuetracker.repository.CategoryRepository;
import com.enterprise.issuetracker.repository.CommentRepository;
import com.enterprise.issuetracker.repository.PriorityRepository;
import com.enterprise.issuetracker.repository.StatusRepository;
import com.enterprise.issuetracker.repository.TicketRepository;
import com.enterprise.issuetracker.repository.TicketSpecification;
import com.enterprise.issuetracker.repository.UserRepository;
import com.enterprise.issuetracker.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final ActivityLogRepository activityLogRepository;

    @Autowired
    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            PriorityRepository priorityRepository,
            StatusRepository statusRepository,
            CommentRepository commentRepository,
            AttachmentRepository attachmentRepository,
            ActivityLogRepository activityLogRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Override
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
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .priority(priority)
                .status(status)
                .dueDate(request.getDueDate())
                .reporter(reporter)
                .assignee(assignee)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // Audit Trail entry
        activityLogRepository.save(ActivityLog.builder()
                .ticketId(savedTicket.getId())
                .user(reporter)
                .action("Created ticket: '" + ticket.getTitle() + "'")
                .build());

        return TicketMapper.toResponse(savedTicket);
    }

    @Override
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
            if (isReporter) {
                // Reporter wants to edit. They can only do so if the ticket is NOT closed
                if (ticket.getStatus().isClosedStatus()) {
                    throw new BadRequestException("Cannot update details of a resolved or closed ticket.");
                }
                // Reporter cannot reassign or change priority
                if (!Objects.equals(ticket.getPriority().getId(), request.getPriorityId()) ||
                        (request.getAssigneeId() != null && (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(request.getAssigneeId())))) {
                    throw new AccessDeniedException("Regular users cannot assign tickets or modify priority levels.");
                }
            } else if (isAssignee) {
                // Assignee (who is NOT reporter) can ONLY change the status of the ticket
                if (!Objects.equals(ticket.getTitle(), request.getTitle()) ||
                        !Objects.equals(ticket.getDescription(), request.getDescription()) ||
                        !Objects.equals(ticket.getCategory().getId(), request.getCategoryId()) ||
                        !Objects.equals(ticket.getPriority().getId(), request.getPriorityId()) ||
                        (request.getAssigneeId() != null && !ticket.getAssignee().getId().equals(request.getAssigneeId()))) {
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

    @Override
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

        ticketRepository.delete(ticket);

        activityLogRepository.save(ActivityLog.builder()
                .user(actor)
                .action("Deleted ticket ID #" + id + ": '" + ticket.getTitle() + "'")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        log.info("Fetching ticket by ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + id));
        return TicketMapper.toResponse(ticket);
    }

    @Override
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
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.filterTickets(keyword, categoryId, priorityId, statusId, reporterId, assigneeId),
                pageable
        );

        return tickets.map(TicketMapper::toResponse);
    }

    @Override
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

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getAllActivityLogs(int page, int size) {
        log.info("Fetching activity logs list");
        Pageable pageable = PageRequest.of(page, size);
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ActivityLogMapper::toResponse);
    }
}
