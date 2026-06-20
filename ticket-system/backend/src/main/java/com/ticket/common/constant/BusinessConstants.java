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

    // === Time constants ===
    /** Milliseconds in one minute — used for SLA deadline calculations */
    public static final long MILLIS_PER_MINUTE = 60_000L;

    // === File upload ===
    /** Maximum upload file size: 10 MB in bytes */
    public static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024L;

    // === Notification types ===
    public static final String NOTIF_TICKET_CREATED = "TICKET_CREATED";
    public static final String NOTIF_TICKET_ASSIGNED = "TICKET_ASSIGNED";
    public static final String NOTIF_TICKET_REPLIED = "TICKET_REPLIED";
    public static final String NOTIF_TICKET_RESOLVED = "TICKET_RESOLVED";
    public static final String NOTIF_TICKET_OVERDUE = "TICKET_OVERDUE";

    // === Content length limits ===
    /** Maximum content length for reply templates (characters) */
    public static final int MAX_TEMPLATE_CONTENT_LENGTH = 5000;
    /** Maximum content length for knowledge articles (characters) */
    public static final int MAX_KNOWLEDGE_CONTENT_LENGTH = 50000;
    /** Maximum number of select options for SINGLE_SELECT fields */
    public static final int MAX_SELECT_OPTIONS = 20;
    /** Default display order for new custom fields */
    public static final int DEFAULT_DISPLAY_ORDER = 99;

    // === Field types ===
    public static final String FIELD_TYPE_SINGLE_SELECT = "SINGLE_SELECT";

    // === Default categories ===
    public static final String DEFAULT_TEMPLATE_CATEGORY = "GENERAL";

    // === Scheduler intervals ===
    /** SLA overdue check interval: 5 minutes in milliseconds */
    public static final long SLA_CHECK_INTERVAL_MS = 300_000L;

    // === Rate limiting ===
    /** Rate limit sliding window in seconds */
    public static final long RATE_LIMIT_WINDOW_SECONDS = 60L;

    // === Pagination defaults ===
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    // === Survey status ===
    public static final String SURVEY_STATUS_DRAFT = "DRAFT";
    public static final String SURVEY_STATUS_PUBLISHED = "PUBLISHED";
    public static final String SURVEY_STATUS_ARCHIVED = "ARCHIVED";

    // === Survey instance status ===
    public static final String INSTANCE_STATUS_READY = "READY_TO_START";
    public static final String INSTANCE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String INSTANCE_STATUS_SUBMITTED = "SUBMITTED";
    public static final String INSTANCE_STATUS_COMPLETED = "COMPLETED";

    // === Survey trigger types ===
    public static final String SURVEY_TRIGGER_TICKET = "TICKET";
    public static final String SURVEY_TRIGGER_MANUAL = "MANUAL";

    // === Survey visibility target types ===
    public static final String SURVEY_TARGET_PAGE = "PAGE";
    public static final String SURVEY_TARGET_SECTION = "SECTION";
    public static final String SURVEY_TARGET_QUESTION = "QUESTION";

    // === Survey question types ===
    public static final String QTYPE_SINGLE_CHOICE = "SINGLE_CHOICE";
    public static final String QTYPE_MULTI_CHOICE = "MULTI_CHOICE";
    public static final String QTYPE_TEXT = "TEXT";
    public static final String QTYPE_TEXTAREA = "TEXTAREA";
    public static final String QTYPE_DATE = "DATE";
    public static final String QTYPE_DROPDOWN = "DROPDOWN";
    public static final String QTYPE_CASCADER = "CASCADER";
    public static final String QTYPE_RATING = "RATING";
    public static final String QTYPE_TABLE = "TABLE";
}
