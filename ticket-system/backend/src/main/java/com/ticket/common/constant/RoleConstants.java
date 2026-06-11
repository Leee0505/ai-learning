package com.ticket.common.constant;

/**
 * Centralized role string constants.
 * Used for both database storage (ROLE_ prefix) and Spring Security hasRole checks (no prefix).
 */
public final class RoleConstants {

    private RoleConstants() {
    }

    // Database-stored role values (with ROLE_ prefix for Spring Security compatibility)
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_AGENT = "ROLE_AGENT";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // Spring Security hasRole shorthand (without ROLE_ prefix)
    // Usage: @PreAuthorize("hasRole('" + ADMIN + "')")
    public static final String ADMIN = "ADMIN";
    public static final String AGENT = "AGENT";
    public static final String USER = "USER";
}
