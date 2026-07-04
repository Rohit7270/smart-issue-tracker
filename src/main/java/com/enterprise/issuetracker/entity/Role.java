package com.enterprise.issuetracker.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing user roles in the database.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, unique = true, nullable = false)
    private RoleType name;
}
