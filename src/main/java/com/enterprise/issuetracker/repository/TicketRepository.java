package com.enterprise.issuetracker.repository;

import com.enterprise.issuetracker.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // Count tickets by status closed flag
    long countByStatusClosedStatus(boolean closedStatus);

    // Count tickets by priority name and closed state
    long countByPriorityNameAndStatusClosedStatus(String priorityName, boolean closedStatus);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.priority.weight >= 3 AND t.status.closedStatus = false")
    long countActiveHighPriorityTickets();

    // Dynamic filtering using JpaSpecificationExecutor
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    // Dashboard: Ticket count by category
    @Query("SELECT t.category.name as categoryName, COUNT(t) as count FROM Ticket t GROUP BY t.category.name")
    List<Map<String, Object>> countTicketsByCategory();

    // Dashboard: Ticket count by status
    @Query("SELECT t.status.name as statusName, COUNT(t) as count FROM Ticket t GROUP BY t.status.name")
    List<Map<String, Object>> countTicketsByStatus();

    // Dashboard: Ticket count by priority
    @Query("SELECT t.priority.name as priorityName, COUNT(t) as count FROM Ticket t GROUP BY t.priority.name")
    List<Map<String, Object>> countTicketsByPriority();

    // Dashboard: Ticket count by assignee
    @Query("SELECT COALESCE(t.assignee.username, 'Unassigned') as assigneeName, COUNT(t) as count FROM Ticket t GROUP BY t.assignee.username")
    List<Map<String, Object>> countTicketsByAssignee();

    // Dashboard: Monthly ticket count trends using PostgreSQL native queries
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as month, COUNT(id) as count " +
            "FROM tickets " +
            "GROUP BY TO_CHAR(created_at, 'YYYY-MM') " +
            "ORDER BY TO_CHAR(created_at, 'YYYY-MM') ASC", nativeQuery = true)
    List<Map<String, Object>> getMonthlyTicketStatistics();

    // Dashboard: User Productivity (number of tickets completed and avg resolution time in hours)
    @Query(value = "SELECT u.username as username, COUNT(t.id) as resolvedCount, " +
            "ROUND(AVG(EXTRACT(EPOCH FROM (t.updated_at - t.created_at)) / 3600)::numeric, 2) as avgResolutionHours " +
            "FROM tickets t " +
            "JOIN users u ON t.assignee_id = u.id " +
            "JOIN statuses s ON t.status_id = s.id " +
            "WHERE s.closed_status = true " +
            "GROUP BY u.username", nativeQuery = true)
    List<Map<String, Object>> getUserProductivityStatistics();
}
