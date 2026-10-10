package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.AssignmentResponse;
import com.enterprise.issuetracker.entity.Assignment;

public class AssignmentMapper {

    public static AssignmentResponse toResponse(Assignment assignment) {
        if (assignment == null) return null;
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .ticketId(assignment.getTicket() != null ? assignment.getTicket().getId() : null)
                .assignedTo(UserMapper.toResponse(assignment.getAssignedTo()))
                .assignedBy(UserMapper.toResponse(assignment.getAssignedBy()))
                .notes(assignment.getNotes())
                .createdAt(assignment.getCreatedAt())
                .build();
    }
}
