package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class TicketNotFoundException extends BusinessException {
    public TicketNotFoundException() {
        super(ErrorCode.TICKET_NOT_FOUND);
    }
}