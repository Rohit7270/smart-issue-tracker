package com.enterprise.issuetracker.repository;

import com.enterprise.issuetracker.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<Assignment> findByAssignedToIdOrderByCreatedAtDesc(Long userId);
}
