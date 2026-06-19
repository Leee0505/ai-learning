package com.ticket.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiMetricsInterceptor.class);
    private static final int MAX_SAMPLES = 10_000;

    private final ConcurrentLinkedDeque<Long> latencies = new ConcurrentLinkedDeque<>();
    private long requestCount;
    private long errorCount;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("metricsStartTime", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startNanos = (Long) request.getAttribute("metricsStartTime");
        if (startNanos == null) return;

        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        synchronized (this) { requestCount++; }
        if (response.getStatus() >= 400) { synchronized (this) { errorCount++; } }

        latencies.addLast(latencyMs);
        while (latencies.size() > MAX_SAMPLES) {
            latencies.pollFirst();
        }
    }

    public long getRequestCount() { return requestCount; }
    public long getErrorCount() { return errorCount; }

    public long getP50Ms() { return percentile(0.50); }
    public long getP95Ms() { return percentile(0.95); }
    public long getP99Ms() { return percentile(0.99); }

    private long percentile(double p) {
        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        if (sorted.length == 0) return 0;
        int idx = (int) (sorted.length * p);
        if (idx >= sorted.length) idx = sorted.length - 1;
        return sorted[idx];
    }
}
