package com.enterprise.issuetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Smart Issue Tracking & Ticket Management System
 * 
 * Entry point for the Spring Boot Application.
 * This class boots the Embedded Server, triggers Component Scanning,
 * and enables database auditing (automatic timestamp generation).
 */
@SpringBootApplication
@EnableJpaAuditing // Enables Spring Data JPA Auditing to automatically populate @CreatedDate and @LastModifiedDate fields
public class SmartIssueTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartIssueTrackerApplication.class, args);
    }
}
