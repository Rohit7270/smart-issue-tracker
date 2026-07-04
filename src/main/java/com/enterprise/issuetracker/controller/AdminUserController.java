package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.UserResponse;
import com.enterprise.issuetracker.dto.UserRoleUpdateRequest;
import com.enterprise.issuetracker.dto.UserStatusUpdateRequest;
import com.enterprise.issuetracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Administration", description = "Endpoints for User Account management, deactivation, and role updates (Admin only)")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminUserController {

    private final UserService userService;

    @Autowired
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get paginated users list", description = "Retrieve a list of all user profiles with pagination and sorting.")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<UserResponse> users = userService.getAllUsers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(users, "Users list retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID", description = "Retrieves profile settings of a user by database ID.")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User details retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle user status (Active/Inactive)", description = "Activates or deactivates user login credentials in the system.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest statusRequest) {
        UserResponse updated = userService.updateUserStatus(id, statusRequest);
        String msg = updated.isActive() ? "User activated successfully" : "User deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(updated, msg));
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Update user roles", description = "Replaces user roles. E.g. promoting a user to MANAGER or ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest roleRequest) {
        UserResponse updated = userService.updateUserRoles(id, roleRequest);
        return ResponseEntity.ok(ApiResponse.success(updated, "User roles updated successfully"));
    }
}
