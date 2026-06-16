package com.ticket.common.constant;

public enum ErrorCode {
    // 4xxxx Client Errors
    VALIDATION_ERROR(40000, "validation error"),
    USERNAME_ALREADY_EXISTS(40001, "username already exists"),
    EMAIL_ALREADY_EXISTS(40002, "email already exists"),
    INVITE_TOKEN_EXPIRED(40003, "invitation link has expired"),
    INVITE_TOKEN_USED(40004, "invitation link already used"),
    INVITE_TOKEN_NOT_FOUND(40005, "invitation link not found"),
    USER_NOT_FOUND(40006, "user not found"),
    RATE_LIMIT_EXCEEDED(40007, "too many requests, please try again later"),
    TICKET_NOT_FOUND(40008, "ticket not found"),
    TICKET_ACCESS_DENIED(40009, "you do not have permission to access this ticket"),
    TICKET_STATUS_INVALID(40010, "invalid ticket status transition"),
    TICKET_ASSIGN_INVALID(40011, "ticket assignment target must be an agent"),
    TICKET_ATTACHMENT_NOT_FOUND(40012, "attachment not found"),
    TICKET_ATTACHMENT_TOO_LARGE(40013, "attachment exceeds maximum size of 10 MB"),
    TICKET_ATTACHMENT_TYPE_DENIED(40014, "this file type is not allowed"),
    REPLY_NOT_FOUND(40015, "reply not found"),
    TEMPLATE_NOT_FOUND(40021, "template not found"),
    CANNOT_DELETE_SELF(40016, "cannot delete your own account"),
    CANNOT_DISABLE_SELF(40017, "cannot disable your own account"),
    CANNOT_CHANGE_OWN_ROLE(40018, "cannot change your own role"),
    USER_ALREADY_DISABLED(40019, "user account is disabled"),
    ROLE_INVALID(40020, "invalid role specified"),

    // 401xx Auth Errors
    INVALID_CREDENTIALS(40100, "invalid credentials"),
    TOKEN_EXPIRED(40101, "token expired"),
    TOKEN_BLACKLISTED(40102, "token has been revoked"),
    TOKEN_INVALID(40103, "token is invalid"),

    // 403xx Forbidden
    ACCESS_DENIED(40300, "access denied"),

    // 500xx Server Errors
    INTERNAL_ERROR(50000, "internal server error");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
