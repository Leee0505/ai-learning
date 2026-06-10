package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class TokenBlacklistedException extends BusinessException {
    public TokenBlacklistedException() {
        super(ErrorCode.TOKEN_BLACKLISTED);
    }
}
