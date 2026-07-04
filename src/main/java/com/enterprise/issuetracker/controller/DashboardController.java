package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.DashboardStatsResponse;
import com.enterprise.issuetracker.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Dashboard & Analytics", description = "Endpoints for retrieving system metrics, trends, and user workloads (Manager/Admin restricted)")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Retrieve unified dashboard stats", description = "Returns total/open/closed counts, category distributions, priority charts, monthly trends, and user productivity details.")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStatistics() {
        DashboardStatsResponse statistics = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "Dashboard statistics retrieved successfully"));
    }
}
