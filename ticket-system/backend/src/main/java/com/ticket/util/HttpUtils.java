package com.ticket.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Shared HTTP utilities — avoids duplicating getClientIp() across the codebase.
 */
public final class HttpUtils {

    private HttpUtils() {}

    /**
     * Extract the real client IP address from the current HTTP request.
     * Checks X-Forwarded-For header first (for proxies/load balancers),
     * then falls back to request.getRemoteAddr().
     */
    public static String getClientIp() {
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
        } catch (Exception ignored) {
            // Not in a web request context
        }
        return "unknown";
    }
}
