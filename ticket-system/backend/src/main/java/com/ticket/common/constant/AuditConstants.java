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
    public static final String ACTION_CREATE_TICKET = "CREATE_TICKET";
    public static final String ACTION_UPDATE_TICKET = "UPDATE_TICKET";
    public static final String ACTION_DELETE_TICKET = "DELETE_TICKET";
    public static final String ACTION_CHANGE_TICKET_STATUS = "CHANGE_TICKET_STATUS";
    public static final String ACTION_ASSIGN_TICKET = "ASSIGN_TICKET";
    public static final String ACTION_REPLY_TICKET = "REPLY_TICKET";
    public static final String ACTION_UPLOAD_ATTACHMENT = "UPLOAD_ATTACHMENT";

    // === Target entity types ===
    public static final String TARGET_USER = "USER";
    public static final String TARGET_INVITE_TOKEN = "INVITE_TOKEN";
    public static final String TARGET_TICKET = "TICKET";
    public static final String TARGET_TICKET_REPLY = "TICKET_REPLY";
    public static final String TARGET_TICKET_ATTACHMENT = "TICKET_ATTACHMENT";
}
