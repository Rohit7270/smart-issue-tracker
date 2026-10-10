package com.enterprise.issuetracker.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {
    private Long id;
    private Long ticketId;
    private UserResponse assignedTo;
    private UserResponse assignedBy;
    private String notes;
    private LocalDateTime createdAt;
}
