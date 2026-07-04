package com.enterprise.issuetracker.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing ticket statuses (e.g., Open, In Progress, Resolved, Closed).
 * Contains a boolean flag indicating if this state represents a finalized/closed state.
 */
@Entity
@Table(name = "statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "closed_status", nullable = false)
    private boolean closedStatus = false; // flag indicating if the ticket is closed/resolved in this status
}
