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
    REPLY_TEMPLATE_NOT_FOUND(40021, "reply template not found"),
    REPLY_TEMPLATE_TITLE_DUPLICATE(40022, "a reply template with this title already exists"),
    KNOWLEDGE_NOT_FOUND(40023, "knowledge article not found"),
    FIELD_KEY_DUPLICATE(40024, "a field with this key already exists"),
    FIELD_NOT_FOUND(40025, "field config not found"),
    SLA_NOT_FOUND(40026, "SLA config not found"),
    SLA_RESPONSE_MUST_BE_LESS_THAN_RESOLUTION(40027, "response minutes must be less than resolution minutes"),
    SLA_PRIORITY_ORDER_VIOLATED(40028, "SLA priority order violated: stricter priorities must have lower hour values than looser ones"),
    CANNOT_DELETE_SELF(40016, "cannot delete your own account"),
    CANNOT_DISABLE_SELF(40017, "cannot disable your own account"),
    CANNOT_CHANGE_OWN_ROLE(40018, "cannot change your own role"),
    USER_ALREADY_DISABLED(40019, "user account is disabled"),
    ROLE_INVALID(40020, "invalid role specified"),

    // 4003x Survey Errors
    TEMPLATE_NOT_FOUND(40030, "survey template not found"),
    TEMPLATE_ALREADY_PUBLISHED(40031, "published templates cannot be deleted"),
    TEMPLATE_VERSION_CONFLICT(40032, "only one published version allowed per template chain"),
    INSTANCE_NOT_FOUND(40033, "survey instance not found"),
    INSTANCE_ALREADY_SUBMITTED(40034, "survey instance already submitted"),
    SURVEY_PAGE_INCOMPLETE(40035, "please complete all required questions on this page"),
    SURVEY_CYCLE_DETECTED(40036, "visibility rules contain a cycle — please adjust"),
    SURVEY_PAGE_NOT_FOUND(40037, "survey instance page not found"),
    PAGE_NOT_FOUND(40038, "survey page not found"),
    SECTION_NOT_FOUND(40039, "survey section not found"),
    QUESTION_NOT_FOUND(40040, "survey question not found"),

    // 4004x Template / Knowledge Errors
    TEMPLATE_CONTENT_TOO_LONG(40041, "template content exceeds maximum length"),
    KNOWLEDGE_CONTENT_TOO_LONG(40042, "knowledge article content exceeds maximum length"),
    KNOWLEDGE_TITLE_DUPLICATE(40043, "a knowledge article with this title already exists"),
    SYSTEM_DEFAULT_WRITE_DENIED(40044, "only superadmin can modify system defaults"),

    // 4005x Ticket / Assignment Errors
    TICKET_CROSS_TENANT_ASSIGN(40050, "cannot assign to an agent from a different tenant"),
    REPLY_EDIT_PERMISSION_DENIED(40051, "only the reply author can edit"),
    REPLY_DELETE_PERMISSION_DENIED(40052, "only the reply author or admin can delete"),
    TICKET_ASSIGNEE_FILTER_INVALID(40053, "assignee filter must be a valid user ID or 'unassigned'"),
    TICKET_CREATOR_FILTER_INVALID(40054, "creator filter must be a valid user ID"),

    // 4006x User / Config Errors
    INVALID_USER_STATUS(40060, "status must be 0 or 1"),
    FIELD_OPTIONS_REQUIRED(40061, "options are required for this field type"),
    FIELD_OPTIONS_FORMAT_INVALID(40062, "options must contain items array with 1-20 entries"),

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
