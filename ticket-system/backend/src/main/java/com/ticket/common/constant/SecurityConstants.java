package com.ticket.common.constant;

/**
 * Security-related constants shared across filters, controllers, and providers.
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** HTTP Authorization header prefix: {@code Bearer } (includes trailing space) */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Length of the Bearer prefix string */
    public static final int BEARER_PREFIX_LENGTH = 7;
}
