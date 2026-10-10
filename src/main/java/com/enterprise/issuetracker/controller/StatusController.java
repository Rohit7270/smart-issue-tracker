package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.StatusDto;
import com.enterprise.issuetracker.service.StatusService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing ticket workflow statuses (e.g. Open, In Progress, Resolved, Closed).
 * Reading statuses is open to all users; write actions require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/statuses")
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    // 1. Create a new status (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StatusDto>> createStatus(@Valid @RequestBody StatusDto statusDto) {
        StatusDto created = statusService.createStatus(statusDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Status created successfully"));
    }

    // 2. Get list of all statuses
    @GetMapping
    public ResponseEntity<ApiResponse<List<StatusDto>>> getAllStatuses() {
        List<StatusDto> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(ApiResponse.success(statuses, "Statuses retrieved successfully"));
    }

    // 3. Get status by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StatusDto>> getStatusById(@PathVariable Long id) {
        StatusDto status = statusService.getStatusById(id);
        return ResponseEntity.ok(ApiResponse.success(status, "Status retrieved successfully"));
    }

    // 4. Update status by ID (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StatusDto>> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDto statusDto) {
        StatusDto updated = statusService.updateStatus(id, statusDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Status updated successfully"));
    }

    // 5. Delete status by ID (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStatus(@PathVariable Long id) {
        statusService.deleteStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status deleted successfully"));
    }
}
