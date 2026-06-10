package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class InviteTokenNotFoundException extends BusinessException {
    public InviteTokenNotFoundException() {
        super(ErrorCode.INVITE_TOKEN_NOT_FOUND);
    }
}
