package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.PriorityDto;
import com.enterprise.issuetracker.service.PriorityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing ticket priorities (e.g. Low, Medium, High, Critical).
 * Reading priorities is open to all users; write actions require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/priorities")
public class PriorityController {

    private final PriorityService priorityService;

    @Autowired
    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    // 1. Create a new priority level (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PriorityDto>> createPriority(@Valid @RequestBody PriorityDto priorityDto) {
        PriorityDto created = priorityService.createPriority(priorityDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Priority created successfully"));
    }

    // 2. Get list of all priority levels
    @GetMapping
    public ResponseEntity<ApiResponse<List<PriorityDto>>> getAllPriorities() {
        List<PriorityDto> priorities = priorityService.getAllPriorities();
        return ResponseEntity.ok(ApiResponse.success(priorities, "Priorities retrieved successfully"));
    }

    // 3. Get priority level by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PriorityDto>> getPriorityById(@PathVariable Long id) {
        PriorityDto priority = priorityService.getPriorityById(id);
        return ResponseEntity.ok(ApiResponse.success(priority, "Priority retrieved successfully"));
    }

    // 4. Update an existing priority level (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PriorityDto>> updatePriority(@PathVariable Long id, @Valid @RequestBody PriorityDto priorityDto) {
        PriorityDto updated = priorityService.updatePriority(id, priorityDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Priority updated successfully"));
    }

    // 5. Delete priority level by ID (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePriority(@PathVariable Long id) {
        priorityService.deletePriority(id);
        return ResponseEntity.ok(ApiResponse.success("Priority deleted successfully"));
    }
}
