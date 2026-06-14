package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class TicketAccessDeniedException extends BusinessException {
    public TicketAccessDeniedException() {
        super(ErrorCode.TICKET_ACCESS_DENIED);
    }
}