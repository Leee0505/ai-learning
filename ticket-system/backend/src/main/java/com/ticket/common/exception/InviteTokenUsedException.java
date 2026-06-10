package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class InviteTokenUsedException extends BusinessException {
    public InviteTokenUsedException() {
        super(ErrorCode.INVITE_TOKEN_USED);
    }
}
