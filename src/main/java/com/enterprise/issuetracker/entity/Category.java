package com.enterprise.issuetracker.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing ticket categories (e.g., Bug, Feature Request, IT Support).
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;
}
