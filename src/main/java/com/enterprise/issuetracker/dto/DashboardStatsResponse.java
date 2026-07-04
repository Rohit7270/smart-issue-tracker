package com.enterprise.issuetracker.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private long totalTickets;
    private long openTickets;
    private long closedTickets;
    private long highPriorityTickets; // Tickets marked with Critical or High priorities
    private List<Map<String, Object>> ticketsByCategory;
    private List<Map<String, Object>> ticketsByPriority;
    private List<Map<String, Object>> ticketsByStatus;
    private List<Map<String, Object>> ticketsByAssignee;
    private List<Map<String, Object>> monthlyTicketStatistics;
    private List<Map<String, Object>> userProductivity;
}
