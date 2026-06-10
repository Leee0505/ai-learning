package com.ticket.common.aspect;

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

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.login(..))")
    public void logLogin(JoinPoint joinPoint) {
        writeAudit("LOGIN", "USER", null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.logout(..))")
    public void logLogout(JoinPoint joinPoint) {
        writeAudit("LOGOUT", "USER", null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.register(..))")
    public void logRegister(JoinPoint joinPoint) {
        writeAudit("REGISTER", "USER", null, null);
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.AuthServiceImpl.invite(..))",
            returning = "result")
    public void logInvite(JoinPoint joinPoint, Object result) {
        writeAudit("INVITE_AGENT", "INVITE_TOKEN", null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.acceptInvite(..))")
    public void logAcceptInvite(JoinPoint joinPoint) {
        writeAudit("ACCEPT_INVITE", "USER", null, null);
    }

    private void writeAudit(String action, String targetType, Long targetId, String detail) {
        try {
            Long userId = getCurrentUserId();
            String ip = getClientIp();

            log.info("AUDIT: userId={}, action={}, targetType={}, targetId={}, ip={}",
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
