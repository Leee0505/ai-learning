package com.ticket.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.ticket.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared security utilities — avoids duplicating getCurrentUserId() across the codebase.
 */
@Slf4j
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
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl udi) {
                    return udi.getTenantId() != null ? udi.getTenantId() : 1L;
            }
        } catch (Exception ignored) {
            log.info("[SECURITY-UTILS] exception, defaulting to 1");
        }
        return 1L;
    }

    /**
     * Check whether the current authenticated user has ROLE_ADMIN.
     * Safe to call in non-web contexts (returns false).
     */
    public static boolean isAdmin() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities() != null
                    && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the current user's tenant ID without defaulting null to 1.
     * Returns null for superadmin (tenant_id=NULL) and when no auth context is available.
     */
    public static Long getCurrentTenantIdOrNull() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl udi) {
                return udi.getTenantId();
            }
        } catch (Exception ignored) {
            // No authentication context available
        }
        return null;
    }

    /**
     * Add tenant_id condition to a query wrapper, handling NULL correctly.
     * When tenantId is null, uses IS NULL; otherwise uses = tenantId.
     */
    public static <T> void tenantEqOrNull(LambdaQueryWrapper<T> wrapper,
                                           SFunction<T, ?> column, Long tenantId) {
        if (tenantId == null) {
            wrapper.isNull(column);
        } else {
            wrapper.eq(column, tenantId);
        }
    }
}
