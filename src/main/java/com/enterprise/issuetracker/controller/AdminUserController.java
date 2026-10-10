package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.UserResponse;
import com.enterprise.issuetracker.dto.UserRoleUpdateRequest;
import com.enterprise.issuetracker.dto.UserStatusUpdateRequest;
import com.enterprise.issuetracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user management (Admin only).
 * Demonstrates role-based authorization with @PreAuthorize.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @Autowired
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Get all users with pagination
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<UserResponse> users = userService.getAllUsers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(users, "Users list retrieved successfully"));
    }

    // 2. Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User details retrieved successfully"));
    }

    // 3. Activate or deactivate a user
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest statusRequest) {
        UserResponse updated = userService.updateUserStatus(id, statusRequest);
        String msg = updated.isActive() ? "User activated successfully" : "User deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(updated, msg));
    }

    // 4. Update user roles (e.g. promote USER to MANAGER or ADMIN)
    @PatchMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest roleRequest) {
        UserResponse updated = userService.updateUserRoles(id, roleRequest);
        return ResponseEntity.ok(ApiResponse.success(updated, "User roles updated successfully"));
    }
}
