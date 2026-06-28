package com.ticket.common.aspect;

import com.ticket.common.constant.AuditConstants;
import com.ticket.dto.response.SurveyInstanceResponse;
import com.ticket.dto.response.TicketAttachmentResponse;
import com.ticket.dto.response.TicketDetailResponse;
import com.ticket.dto.response.TicketReplyResponse;
import com.ticket.entity.AuditLog;
import com.ticket.mapper.AuditLogMapper;
import com.ticket.util.HttpUtils;
import com.ticket.util.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogMapper auditLogMapper;

    public AuditLogAspect(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.login(..))")
    public void logLogin(JoinPoint joinPoint) {
        writeAudit(AuditConstants.ACTION_LOGIN, AuditConstants.TARGET_USER, null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.logout(..))")
    public void logLogout(JoinPoint joinPoint) {
        writeAudit(AuditConstants.ACTION_LOGOUT, AuditConstants.TARGET_USER, null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.register(..))")
    public void logRegister(JoinPoint joinPoint) {
        writeAudit(AuditConstants.ACTION_REGISTER, AuditConstants.TARGET_USER, null, null);
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.AuthServiceImpl.invite(..))",
            returning = "result")
    public void logInvite(JoinPoint joinPoint, Object result) {
        writeAudit(AuditConstants.ACTION_INVITE_AGENT, AuditConstants.TARGET_INVITE_TOKEN, null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.acceptInvite(..))")
    public void logAcceptInvite(JoinPoint joinPoint) {
        writeAudit(AuditConstants.ACTION_ACCEPT_INVITE, AuditConstants.TARGET_USER, null, null);
    }

    // === Ticket operations ===

    @AfterReturning(value = "execution(* com.ticket.service.impl.TicketServiceImpl.createTicket(..))",
            returning = "result")
    public void logCreateTicket(JoinPoint joinPoint, Object result) {
        if (result instanceof TicketDetailResponse r) {
            writeAudit(AuditConstants.ACTION_CREATE_TICKET, AuditConstants.TARGET_TICKET, r.getId(), "Ticket created");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.TicketServiceImpl.updateTicket(..))",
            returning = "result")
    public void logUpdateTicket(JoinPoint joinPoint, Object result) {
        if (result instanceof TicketDetailResponse r) {
            writeAudit(AuditConstants.ACTION_UPDATE_TICKET, AuditConstants.TARGET_TICKET, r.getId(), "Ticket updated");
        }
    }

    @AfterReturning("execution(* com.ticket.service.impl.TicketServiceImpl.deleteTicket(..)) && args(ticketId)")
    public void logDeleteTicket(Long ticketId) {
        writeAudit(AuditConstants.ACTION_DELETE_TICKET, AuditConstants.TARGET_TICKET, ticketId, "Ticket deleted");
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.TicketServiceImpl.changeStatus(..))",
            returning = "result")
    public void logChangeTicketStatus(JoinPoint joinPoint, Object result) {
        if (result instanceof TicketDetailResponse r) {
            writeAudit(AuditConstants.ACTION_CHANGE_TICKET_STATUS, AuditConstants.TARGET_TICKET,
                    r.getId(), "Status changed to " + r.getStatus());
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.TicketServiceImpl.assignTicket(..))",
            returning = "result")
    public void logAssignTicket(JoinPoint joinPoint, Object result) {
        if (result instanceof TicketDetailResponse r) {
            writeAudit(AuditConstants.ACTION_ASSIGN_TICKET, AuditConstants.TARGET_TICKET,
                    r.getId(), "Assigned to userId=" + r.getAssignedTo());
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.TicketServiceImpl.addReply(..))",
            returning = "result")
    public void logReplyTicket(JoinPoint joinPoint, Object result) {
        if (result instanceof TicketReplyResponse r) {
            writeAudit(AuditConstants.ACTION_REPLY_TICKET, AuditConstants.TARGET_TICKET_REPLY,
                    r.getId(), r.getIsInternal() ? "Internal note" : "Public reply");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.TicketServiceImpl.uploadAttachment(..))",
            returning = "result")
    public void logUploadAttachment(JoinPoint joinPoint, Object result) {
        if (result instanceof TicketAttachmentResponse r) {
            writeAudit(AuditConstants.ACTION_UPLOAD_ATTACHMENT, AuditConstants.TARGET_TICKET_ATTACHMENT,
                    r.getId(), "File: " + r.getOriginalFilename());
        }
    }

    // === Survey operations ===

    @AfterReturning(value = "execution(* com.ticket.service.impl.SurveyServiceImpl.createInstance(..))",
            returning = "result")
    public void logCreateInstance(JoinPoint joinPoint, Object result) {
        if (result instanceof SurveyInstanceResponse r) {
            writeAudit(AuditConstants.ACTION_CREATE_SURVEY_INSTANCE, AuditConstants.TARGET_SURVEY_INSTANCE,
                    r.getId(), "Instance created");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.SurveyServiceImpl.completePage(..))",
            returning = "result")
    public void logCompletePage(JoinPoint joinPoint, Object result) {
        if (result instanceof SurveyInstanceResponse r) {
            writeAudit(AuditConstants.ACTION_COMPLETE_PAGE, AuditConstants.TARGET_SURVEY_INSTANCE,
                    r.getId(), "Page completed");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.SurveyServiceImpl.reopenPage(..))",
            returning = "result")
    public void logReopenPage(JoinPoint joinPoint, Object result) {
        if (result instanceof SurveyInstanceResponse r) {
            writeAudit(AuditConstants.ACTION_REOPEN_PAGE, AuditConstants.TARGET_SURVEY_INSTANCE,
                    r.getId(), "Page reopened");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.SurveyServiceImpl.reopenInstance(..))",
            returning = "result")
    public void logReopenInstance(JoinPoint joinPoint, Object result) {
        if (result instanceof SurveyInstanceResponse r) {
            writeAudit(AuditConstants.ACTION_REOPEN_INSTANCE, AuditConstants.TARGET_SURVEY_INSTANCE,
                    r.getId(), "Instance reopened");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.SurveyServiceImpl.submitSurvey(..))",
            returning = "result")
    public void logSubmitSurvey(JoinPoint joinPoint, Object result) {
        if (result instanceof SurveyInstanceResponse r) {
            writeAudit(AuditConstants.ACTION_SUBMIT_SURVEY, AuditConstants.TARGET_SURVEY_INSTANCE,
                    r.getId(), "Survey submitted/completed");
        }
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.SurveyServiceImpl.reassignInstance(..))",
            returning = "result")
    public void logReassignInstance(JoinPoint joinPoint, Object result) {
        if (result instanceof SurveyInstanceResponse r) {
            writeAudit(AuditConstants.ACTION_REASSIGN_SURVEY, AuditConstants.TARGET_SURVEY_INSTANCE,
                    r.getId(), "Instance reassigned to userId=" + r.getAssignedTo());
        }
    }

    private void writeAudit(String action, String targetType, Long targetId, String detail) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String ip = HttpUtils.getClientIp();

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setDetail(detail);
            auditLog.setIpAddress(ip);
            auditLogMapper.insert(auditLog);

            log.debug("Audit log written: userId={}, action={}, targetType={}, targetId={}, ip={}",
                    userId, action, targetType, targetId, ip);
        } catch (Exception e) {
            log.warn("Failed to write audit log", e);
        }
    }
}
