package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.PriorityDto;
import com.enterprise.issuetracker.service.PriorityService;
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
@RequestMapping("/api/v1/priorities")
@Tag(name = "Priority Management", description = "Endpoints for configuring ticket priorities (Admin restricted for write actions)")
@SecurityRequirement(name = "Bearer Authentication")
public class PriorityController {

    private final PriorityService priorityService;

    @Autowired
    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new priority level (Admin only)", description = "Adds a new priority level definition containing name and weight.")
    public ResponseEntity<ApiResponse<PriorityDto>> createPriority(@Valid @RequestBody PriorityDto priorityDto) {
        PriorityDto created = priorityService.createPriority(priorityDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Priority created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get list of all priority levels", description = "Returns all available ticket priority levels (e.g. Low, High).")
    public ResponseEntity<ApiResponse<List<PriorityDto>>> getAllPriorities() {
        List<PriorityDto> priorities = priorityService.getAllPriorities();
        return ResponseEntity.ok(ApiResponse.success(priorities, "Priorities retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get priority level by ID", description = "Retrieves a specific priority level record details by ID.")
    public ResponseEntity<ApiResponse<PriorityDto>> getPriorityById(@PathVariable Long id) {
        PriorityDto priority = priorityService.getPriorityById(id);
        return ResponseEntity.ok(ApiResponse.success(priority, "Priority retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing priority level (Admin only)", description = "Modifies priority name or weight metadata.")
    public ResponseEntity<ApiResponse<PriorityDto>> updatePriority(@PathVariable Long id, @Valid @RequestBody PriorityDto priorityDto) {
        PriorityDto updated = priorityService.updatePriority(id, priorityDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Priority updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete priority level by ID (Admin only)", description = "Deletes a priority definition from the database lookup table.")
    public ResponseEntity<ApiResponse<Void>> deletePriority(@PathVariable Long id) {
        priorityService.deletePriority(id);
        return ResponseEntity.ok(ApiResponse.success("Priority deleted successfully"));
    }
}
