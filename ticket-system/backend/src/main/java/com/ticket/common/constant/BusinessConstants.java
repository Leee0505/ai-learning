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

    // === Ticket status ===
    public static final String TICKET_STATUS_OPEN = "OPEN";
    public static final String TICKET_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String TICKET_STATUS_RESOLVED = "RESOLVED";
    public static final String TICKET_STATUS_CLOSED = "CLOSED";

    // === Ticket priority ===
    public static final String TICKET_PRIORITY_LOW = "LOW";
    public static final String TICKET_PRIORITY_MEDIUM = "MEDIUM";
    public static final String TICKET_PRIORITY_HIGH = "HIGH";
    public static final String TICKET_PRIORITY_URGENT = "URGENT";

    /** SLA priority ordering: strictest → loosest. Used for cross-priority boundary validation. */
    public static final java.util.List<String> SLA_PRIORITY_ORDER =
            java.util.List.of(TICKET_PRIORITY_URGENT, TICKET_PRIORITY_HIGH, TICKET_PRIORITY_MEDIUM, TICKET_PRIORITY_LOW);

    // === Ticket category ===
    public static final String TICKET_CATEGORY_BUG = "BUG";
    public static final String TICKET_CATEGORY_FEATURE_REQUEST = "FEATURE_REQUEST";
    public static final String TICKET_CATEGORY_GENERAL_QUESTION = "GENERAL_QUESTION";
    public static final String TICKET_CATEGORY_ACCOUNT_ISSUE = "ACCOUNT_ISSUE";
    public static final String TICKET_CATEGORY_OTHER = "OTHER";

    // === File upload ===
    /** Maximum upload file size: 10 MB in bytes */
    public static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024L;

    // === Pagination defaults ===
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
}
