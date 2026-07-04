package com.enterprise.issuetracker.entity;

/**
 * Enumeration representing security roles in the application.
 * Prepended with "ROLE_" to match Spring Security standard prefixing.
 */
public enum RoleType {
    ROLE_ADMIN,
    ROLE_MANAGER,
    ROLE_USER
}
