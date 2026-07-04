package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.StatusDto;
import com.enterprise.issuetracker.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statuses")
@Tag(name = "Status Management", description = "Endpoints for configuring ticket statuses (Admin restricted for write actions)")
@SecurityRequirement(name = "Bearer Authentication")
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new ticket status (Admin only)", description = "Adds a status definition (e.g. Open, Resolved) with a closed state flag.")
    public ResponseEntity<ApiResponse<StatusDto>> createStatus(@Valid @RequestBody StatusDto statusDto) {
        StatusDto created = statusService.createStatus(statusDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Status created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get list of all ticket statuses", description = "Returns all available ticket state definitions.")
    public ResponseEntity<ApiResponse<List<StatusDto>>> getAllStatuses() {
        List<StatusDto> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(ApiResponse.success(statuses, "Statuses retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get status level by ID", description = "Retrieves a specific status definition details by database ID.")
    public ResponseEntity<ApiResponse<StatusDto>> getStatusById(@PathVariable Long id) {
        StatusDto status = statusService.getStatusById(id);
        return ResponseEntity.ok(ApiResponse.success(status, "Status retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing status (Admin only)", description = "Modifies status name or its resolved/closed state flag.")
    public ResponseEntity<ApiResponse<StatusDto>> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDto statusDto) {
        StatusDto updated = statusService.updateStatus(id, statusDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete status by ID (Admin only)", description = "Deletes a status definition from database lookup table.")
    public ResponseEntity<ApiResponse<Void>> deleteStatus(@PathVariable Long id) {
        statusService.deleteStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status deleted successfully"));
    }
}
