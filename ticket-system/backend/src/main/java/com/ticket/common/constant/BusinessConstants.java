package com.ticket.common.constant;

/**
 * Business-level magic numbers and configuration constants.
 * Grouped by domain to keep related values together.
 */
public final class BusinessConstants {

    private BusinessConstants() {
    }

    // === User status ===
    /** Account is enabled and can log in */
    public static final int USER_STATUS_ENABLED = 1;
    /** Account is disabled — login blocked */
    public static final int USER_STATUS_DISABLED = 0;

    // === Invite token ===
    /** Invite link validity period: 48 hours in milliseconds */
    public static final long INVITE_EXPIRY_MS = 48 * 60 * 60 * 1000L;
    /** Invite token has not been consumed */
    public static final int INVITE_TOKEN_UNUSED = 0;
    /** Invite token has been consumed */
    public static final int INVITE_TOKEN_USED = 1;
}
