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

/**
 * Spring Data JPA Repository for Ticket entity.
 *
 * By extending JpaRepository<Ticket, Long>, Spring automatically generates
 * standard CRUD methods: save(), findById(), findAll(), deleteById().
 *
 * Extending JpaSpecificationExecutor<Ticket> allows dynamic filtering with Specifications.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // Derived Query Method: Spring creates SQL from method name automatically
    long countByStatusClosedStatus(boolean closedStatus);

    // Derived Query Method: Count by priority name and status closed flag
    long countByPriorityNameAndStatusClosedStatus(String priorityName, boolean closedStatus);

    // Simple derived query methods for finding tickets
    List<Ticket> findByStatusId(Long statusId);

    List<Ticket> findByAssigneeId(Long assigneeId);

    List<Ticket> findByReporterId(Long reporterId);

    List<Ticket> findByCategoryId(Long categoryId);

    // Simple JPQL Query: Count active high priority tickets
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.priority.weight >= 3 AND t.status.closedStatus = false")
    long countActiveHighPriorityTickets();

    // Dynamic filtering using JpaSpecificationExecutor
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    // Simple JPQL Query: Count tickets grouped by category name
    @Query("SELECT t.category.name as categoryName, COUNT(t) as count FROM Ticket t GROUP BY t.category.name")
    List<Map<String, Object>> countTicketsByCategory();

    // Simple JPQL Query: Count tickets grouped by status name
    @Query("SELECT t.status.name as statusName, COUNT(t) as count FROM Ticket t GROUP BY t.status.name")
    List<Map<String, Object>> countTicketsByStatus();

    // Simple JPQL Query: Count tickets grouped by priority name
    @Query("SELECT t.priority.name as priorityName, COUNT(t) as count FROM Ticket t GROUP BY t.priority.name")
    List<Map<String, Object>> countTicketsByPriority();

    // Simple JPQL Query: Count tickets grouped by assignee
    @Query("SELECT COALESCE(t.assignee.username, 'Unassigned') as assigneeName, COUNT(t) as count FROM Ticket t GROUP BY t.assignee.username")
    List<Map<String, Object>> countTicketsByAssignee();
}
