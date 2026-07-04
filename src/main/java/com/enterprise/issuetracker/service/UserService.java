package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.UserResponse;
import com.enterprise.issuetracker.dto.UserRoleUpdateRequest;
import com.enterprise.issuetracker.dto.UserStatusUpdateRequest;
import org.springframework.data.domain.Page;

public interface UserService {
    Page<UserResponse> getAllUsers(int page, int size, String sortBy, String sortDir);
    UserResponse getUserById(Long id);
    UserResponse updateUserStatus(Long id, UserStatusUpdateRequest request);
    UserResponse updateUserRoles(Long id, UserRoleUpdateRequest request);
}
