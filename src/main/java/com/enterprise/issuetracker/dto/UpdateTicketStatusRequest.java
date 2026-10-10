package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull(message = "Status ID is required")
    private Long statusId;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
