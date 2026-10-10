package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTicketRequest {

    @NotNull(message = "Assignee user ID is required")
    private Long assigneeId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
