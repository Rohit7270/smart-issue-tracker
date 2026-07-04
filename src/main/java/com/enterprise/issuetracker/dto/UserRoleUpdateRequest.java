package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotEmpty(message = "Roles list cannot be empty")
    private Set<String> roles;
}
