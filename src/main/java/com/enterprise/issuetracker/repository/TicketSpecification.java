package com.enterprise.issuetracker.repository;

import com.enterprise.issuetracker.entity.Ticket;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification utility for dynamic ticket filtering.
 *
 * Instead of writing multiple static queries, a Specification builds the SQL
 * "WHERE" clause dynamically based on which filters are provided by the user.
 */
public class TicketSpecification {

    /**
     * Builds a composite Specification combining all optional search filters.
     * If a filter parameter is null or empty, it is simply skipped.
     */
    public static Specification<Ticket> filterTickets(
            String keyword,
            Long categoryId,
            Long priorityId,
            Long statusId,
            Long reporterId,
            Long assigneeId) {

        return (root, query, criteriaBuilder) -> {
            // List to hold all active WHERE conditions (predicates)
            List<Predicate> predicates = new ArrayList<>();


            // Keyword Search: check if keyword matches title or description (case-insensitive)
            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }

            // Category Filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // Priority Filter
            if (priorityId != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority").get("id"), priorityId));
            }

            // Status Filter
            if (statusId != null) {
                predicates.add(criteriaBuilder.equal(root.get("status").get("id"), statusId));
            }

            // Reporter Filter
            if (reporterId != null) {
                predicates.add(criteriaBuilder.equal(root.get("reporter").get("id"), reporterId));
            }

            // Assignee Filter
            if (assigneeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignee").get("id"), assigneeId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
