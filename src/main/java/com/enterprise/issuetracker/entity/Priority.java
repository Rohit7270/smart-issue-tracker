package com.enterprise.issuetracker.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing ticket priorities (e.g., Low, Medium, High, Critical).
 * Contains a numeric weight to allow sorting and metrics processing.
 */
@Entity
@Table(name = "priorities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Priority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private Integer weight; // Higher weight indicates higher priority (e.g., Critical = 4, Low = 1)
}
