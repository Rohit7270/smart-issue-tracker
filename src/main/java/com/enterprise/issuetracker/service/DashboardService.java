package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.DashboardStatsResponse;
import com.enterprise.issuetracker.entity.Ticket;
import com.enterprise.issuetracker.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Dashboard and Analytics.
 * Aggregates high-level statistics, category/status distributions, and team activity.
 */
@Service
@Slf4j
public class DashboardService {

    private final TicketRepository ticketRepository;

    @Autowired
    public DashboardService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStatistics() {
        log.info("Generating system dashboard statistics");

        // 1. Fetch basic counts using Spring Data JPA repository methods
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatusClosedStatus(false);
        long closed = ticketRepository.countByStatusClosedStatus(true);
        long activeHighPriority = ticketRepository.countActiveHighPriorityTickets();

        // 2. Fetch all tickets for in-memory grouping (easy to understand for beginners)
        List<Ticket> allTickets = ticketRepository.findAll();

        // Group resolved tickets by assignee username
        Map<String, Long> userResolvedMap = allTickets.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().isClosedStatus() && t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getUsername(), Collectors.counting()));

        List<Map<String, Object>> userProductivity = new ArrayList<>();
        userResolvedMap.forEach((username, count) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            map.put("resolvedCount", count);
            userProductivity.add(map);
        });

        // Group created tickets by Year-Month (e.g., "2026-10")
        Map<String, Long> monthlyCountMap = allTickets.stream()
                .filter(t -> t.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toString().substring(0, 7),
                        TreeMap::new,
                        Collectors.counting()
                ));

        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        monthlyCountMap.forEach((month, count) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("month", month);
            map.put("count", count);
            monthlyStats.add(map);
        });

        // 3. Build and return unified Dashboard response DTO
        return DashboardStatsResponse.builder()
                .totalTickets(total)
                .openTickets(open)
                .closedTickets(closed)
                .highPriorityTickets(activeHighPriority)
                .ticketsByCategory(ticketRepository.countTicketsByCategory())
                .ticketsByPriority(ticketRepository.countTicketsByPriority())
                .ticketsByStatus(ticketRepository.countTicketsByStatus())
                .ticketsByAssignee(ticketRepository.countTicketsByAssignee())
                .monthlyTicketStatistics(monthlyStats)
                .userProductivity(userProductivity)
                .build();
    }
}
