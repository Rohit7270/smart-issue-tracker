package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusUpdateRequest {

    @NotNull(message = "Active flag status is required")
    private Boolean active;
}
