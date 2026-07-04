package com.enterprise.issuetracker.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private CategoryDto category;
    private PriorityDto priority;
    private StatusDto status;
    private LocalDate dueDate;
    private UserResponse reporter;
    private UserResponse assignee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;
    private List<AttachmentResponse> attachments;
}
