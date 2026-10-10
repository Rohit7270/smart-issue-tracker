package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.TicketRequest;
import com.enterprise.issuetracker.dto.TicketResponse;
import com.enterprise.issuetracker.entity.Category;
import com.enterprise.issuetracker.entity.Priority;
import com.enterprise.issuetracker.entity.Status;
import com.enterprise.issuetracker.entity.Ticket;
import com.enterprise.issuetracker.entity.User;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Mapper utility for bidirectional conversion between Ticket domain entities and API DTOs.
 */
public class TicketMapper {

    /**
     * Converts a Ticket domain entity to a TicketResponse DTO for API output.
     */
    public static TicketResponse toResponse(Ticket ticket) {
        if (ticket == null) return null;
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .category(CategoryMapper.toDto(ticket.getCategory()))
                .priority(PriorityMapper.toDto(ticket.getPriority()))
                .status(StatusMapper.toDto(ticket.getStatus()))
                .dueDate(ticket.getDueDate())
                .reporter(UserMapper.toResponse(ticket.getReporter()))
                .assignee(UserMapper.toResponse(ticket.getAssignee()))
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .comments(ticket.getComments() == null ? Collections.emptyList() :
                        ticket.getComments().stream().map(CommentMapper::toResponse).collect(Collectors.toList()))
                .attachments(ticket.getAttachments() == null ? Collections.emptyList() :
                        ticket.getAttachments().stream().map(AttachmentMapper::toResponse).collect(Collectors.toList()))
                .assignments(ticket.getAssignments() == null ? Collections.emptyList() :
                        ticket.getAssignments().stream().map(AssignmentMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps an incoming API TicketRequest DTO and resolved entity dependencies into a new Ticket entity.
     */
    public static Ticket toEntity(TicketRequest request, Category category, Priority priority, Status status, User reporter, User assignee) {
        if (request == null) return null;
        return Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .priority(priority)
                .status(status)
                .dueDate(request.getDueDate())
                .reporter(reporter)
                .assignee(assignee)
                .build();
    }
}
