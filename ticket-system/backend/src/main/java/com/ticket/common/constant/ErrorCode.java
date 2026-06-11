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
