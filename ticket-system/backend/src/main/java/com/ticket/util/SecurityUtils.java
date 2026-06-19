package com.ticket.util;

import com.ticket.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared security utilities — avoids duplicating getCurrentUserId() across the codebase.
 */
public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);
    private SecurityUtils() {}

    /**
     * Extract the current authenticated user's ID from SecurityContext.
     * Returns 0 if no authentication context is available (e.g. system operation).
     */
    public static Long getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl principal) {
                return principal.getUserId();
            }
        } catch (Exception ignored) {
            // No authentication context available
        }
        return 0L;
    }

    /**
     * Extract the current authenticated user's tenant ID from SecurityContext.
     * Returns 1 (default tenant) if no authentication context is available.
     */
    public static Long getCurrentTenantId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                var principal = auth.getPrincipal();
                log.info("[SECURITY-UTILS] principal type={}, value={}",
                        principal != null ? principal.getClass().getSimpleName() : "null",
                        principal);
                if (principal instanceof UserDetailsImpl udi) {
                    Long tenantId = udi.getTenantId();
                    log.info("[SECURITY-UTILS] getCurrentTenantId() = {}", tenantId);
                    return tenantId != null ? tenantId : 1L;
                }
                log.info("[SECURITY-UTILS] principal not UserDetailsImpl, defaulting to 1");
            } else {
                log.info("[SECURITY-UTILS] no authentication, defaulting to 1");
            }
        } catch (Exception ignored) {
            log.info("[SECURITY-UTILS] exception, defaulting to 1");
        }
        return 1L;
    }
}
