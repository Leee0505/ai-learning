package com.ticket.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.constant.ErrorCode;
import com.ticket.dto.response.ApiResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simple IP-based rate limiting filter for auth endpoints.
 * Uses Redis atomic counters with TTL to track requests per IP per endpoint.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Rate-limit rules: path pattern → max requests per minute
    private static final Map<String, Integer> RATE_LIMIT_RULES = Map.of(
            "/api/auth/login", 10,
            "/api/auth/register", 3,
            "/api/tickets/create", 20,
            "/api/tickets/export", 2
    );
    // Attachment upload: rate-limited by path prefix
    private static final int ATTACHMENT_LIMIT_PER_MINUTE = 10;

    private final RedissonClient redissonClient;

    @Value("${rate-limit.max-requests-per-minute:10}")
    private int defaultMaxRequestsPerMinute;

    public RateLimitFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        int maxRequests = 0;

        // Check exact path + method matches
        if ("POST".equals(method) && path.equals("/api/tickets")) {
            maxRequests = RATE_LIMIT_RULES.getOrDefault("/api/tickets/create", 0);
        } else if ("GET".equals(method) && path.startsWith("/api/tickets/export")) {
            maxRequests = RATE_LIMIT_RULES.getOrDefault("/api/tickets/export", 0);
        } else if ("POST".equals(method) && path.contains("/attachments")) {
            maxRequests = ATTACHMENT_LIMIT_PER_MINUTE;
        } else {
            maxRequests = RATE_LIMIT_RULES.getOrDefault(path, 0);
        }

        if (maxRequests <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String rateLimitKey = "rate-limit:" + path + ":" + clientIp;

        RAtomicLong counter = redissonClient.getAtomicLong(rateLimitKey);
        long count = counter.incrementAndGet();

        if (count == 1) {
            counter.expire(60, TimeUnit.SECONDS);
        }

        if (count > maxRequests) {
            log.warn("Rate limit exceeded: ip={}, path={}, count={}", clientIp, path, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResult.error(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                            ErrorCode.RATE_LIMIT_EXCEEDED.getDefaultMessage()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
