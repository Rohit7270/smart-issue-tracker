package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.TicketResponse;
import com.enterprise.issuetracker.entity.Ticket;

import java.util.Collections;
import java.util.stream.Collectors;

public class TicketMapper {

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
                .build();
    }
}
