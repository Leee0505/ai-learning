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
import java.util.concurrent.TimeUnit;

/**
 * Simple IP-based rate limiting filter for auth endpoints.
 * Uses Redis atomic counters with TTL to track requests per IP per endpoint.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RedissonClient redissonClient;

    @Value("${rate-limit.max-requests-per-minute:10}")
    private int maxRequestsPerMinute;

    public RateLimitFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only rate-limit login and register endpoints
        if (!path.equals("/api/auth/login") && !path.equals("/api/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String rateLimitKey = "rate-limit:" + path + ":" + clientIp;

        RAtomicLong counter = redissonClient.getAtomicLong(rateLimitKey);
        long count = counter.incrementAndGet();

        // Set TTL on first request in the window
        if (count == 1) {
            counter.expire(60, TimeUnit.SECONDS);
        }

        if (count > maxRequestsPerMinute) {
            log.warn("Rate limit exceeded: ip={}, path={}, count={}", clientIp, path, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResult<Void> errorResponse = ApiResult.error(
                    ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                    ErrorCode.RATE_LIMIT_EXCEEDED.getDefaultMessage());
            objectMapper.writeValue(response.getWriter(), errorResponse);
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
