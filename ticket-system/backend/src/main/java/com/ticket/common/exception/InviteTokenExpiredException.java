package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class InviteTokenExpiredException extends BusinessException {
    public InviteTokenExpiredException() {
        super(ErrorCode.INVITE_TOKEN_EXPIRED);
    }
}
