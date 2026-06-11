package com.ticket.common.constant;

/**
 * Audit log action types and target entity types.
 * Keeps the audit trail vocabulary consistent across the entire application.
 */
public final class AuditConstants {

    private AuditConstants() {
    }

    // === Action types ===
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_INVITE_AGENT = "INVITE_AGENT";
    public static final String ACTION_ACCEPT_INVITE = "ACCEPT_INVITE";

    // === Target entity types ===
    public static final String TARGET_USER = "USER";
    public static final String TARGET_INVITE_TOKEN = "INVITE_TOKEN";
}
