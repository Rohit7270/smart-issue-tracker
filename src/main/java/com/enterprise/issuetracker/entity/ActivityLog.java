package com.enterprise.issuetracker.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing activity logs for audit trails.
 */
@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId; // linked ticket, nullable for non-ticket events

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // user who triggered the action, nullable for system actions

    @Column(nullable = false, length = 1000)
    private String action; // description of the action (e.g., "Status updated to Closed")
}
