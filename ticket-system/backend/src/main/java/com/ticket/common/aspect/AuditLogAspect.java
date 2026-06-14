package com.ticket.common.aspect;

import com.ticket.common.constant.AuditConstants;
import com.ticket.dto.response.TicketAttachmentResponse;
import com.ticket.dto.response.TicketDetailResponse;
import com.ticket.dto.response.TicketReplyResponse;
import com.ticket.entity.AuditLog;
import com.ticket.mapper.AuditLogMapper;
import com.ticket.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    private void writeAudit(String action, String targetType, Long targetId, String detail) {
        try {
            Long userId = getCurrentUserId();
            String ip = getClientIp();

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

    private Long getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl principal) {
                return principal.getUserId();
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
