package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.ApiResponse;
import com.enterprise.issuetracker.dto.DashboardStatsResponse;
import com.enterprise.issuetracker.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving dashboard analytics (Manager and Admin only).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Retrieve high-level statistics and breakdown counts
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStatistics() {
        DashboardStatsResponse statistics = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "Dashboard statistics retrieved successfully"));
    }
}
