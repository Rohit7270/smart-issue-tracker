package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusDto {
    private Long id;

    @NotBlank(message = "Status name is required")
    @Size(min = 2, max = 30, message = "Status name must be between 2 and 30 characters")
    private String name;

    private boolean closedStatus;
}
