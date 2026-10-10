package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.UserResponse;
import com.enterprise.issuetracker.dto.UserRoleUpdateRequest;
import com.enterprise.issuetracker.dto.UserStatusUpdateRequest;
import com.enterprise.issuetracker.entity.Role;
import com.enterprise.issuetracker.entity.RoleType;
import com.enterprise.issuetracker.entity.User;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.mapper.UserMapper;
import com.enterprise.issuetracker.repository.RoleRepository;
import com.enterprise.issuetracker.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching all users with pagination - page: {}, size: {}", page, size);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UserStatusUpdateRequest request) {
        log.info("Updating user status (active: {}) for user ID: {}", request.getActive(), id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setActive(request.getActive());
        User updated = userRepository.save(user);
        return UserMapper.toResponse(updated);
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, UserRoleUpdateRequest request) {
        log.info("Updating user roles for user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        Set<Role> roles = new HashSet<>();
        request.getRoles().forEach(roleName -> {
            switch (roleName.toUpperCase()) {
                case "ADMIN":
                    Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                            .orElseThrow(() -> new ResourceNotFoundException("Error: Role ADMIN is not found."));
                    roles.add(adminRole);
                    break;
                case "MANAGER":
                    Role managerRole = roleRepository.findByName(RoleType.ROLE_MANAGER)
                            .orElseThrow(() -> new ResourceNotFoundException("Error: Role MANAGER is not found."));
                    roles.add(managerRole);
                    break;
                case "USER":
                    Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                            .orElseThrow(() -> new ResourceNotFoundException("Error: Role USER is not found."));
                    roles.add(userRole);
                    break;
                default:
                    throw new BadRequestException("Role name '" + roleName + "' is invalid. Valid roles: USER, MANAGER, ADMIN");
            }
        });

        user.setRoles(roles);
        User updated = userRepository.save(user);
        return UserMapper.toResponse(updated);
    }
}
