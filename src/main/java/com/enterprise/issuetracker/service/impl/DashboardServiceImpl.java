package com.enterprise.issuetracker.service.impl;

import com.enterprise.issuetracker.dto.DashboardStatsResponse;
import com.enterprise.issuetracker.repository.TicketRepository;
import com.enterprise.issuetracker.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;

    @Autowired
    public DashboardServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStatistics() {
        log.info("Generating system dashboard statistics");

        long total = ticketRepository.count();
        long open = ticketRepository.countByStatusClosedStatus(false);
        long closed = ticketRepository.countByStatusClosedStatus(true);
        long activeHighPriority = ticketRepository.countActiveHighPriorityTickets();

        return DashboardStatsResponse.builder()
                .totalTickets(total)
                .openTickets(open)
                .closedTickets(closed)
                .highPriorityTickets(activeHighPriority)
                .ticketsByCategory(ticketRepository.countTicketsByCategory())
                .ticketsByPriority(ticketRepository.countTicketsByPriority())
                .ticketsByStatus(ticketRepository.countTicketsByStatus())
                .ticketsByAssignee(ticketRepository.countTicketsByAssignee())
                .monthlyTicketStatistics(ticketRepository.getMonthlyTicketStatistics())
                .userProductivity(ticketRepository.getUserProductivityStatistics())
                .build();
    }
}
