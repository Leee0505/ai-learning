package com.ticket.util;

import com.ticket.security.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared security utilities — avoids duplicating getCurrentUserId() across the codebase.
 */
public final class SecurityUtils {

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
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl principal) {
                Long tenantId = principal.getTenantId();
                return tenantId != null ? tenantId : 1L;
            }
        } catch (Exception ignored) {
            // No authentication context available
        }
        return 1L;
    }
}
