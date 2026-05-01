package com.deva.auth.observability;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestObservabilityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestObservabilityFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String XRAY_TRACE_HEADER = "X-Amzn-Trace-Id";

    private final long slowRequestThresholdMs;

    RequestObservabilityFilter(@Value("${observability.slow-request-threshold-ms:1000}") long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startedAtNanos = System.nanoTime();
        String requestId = requestId(request);

        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());
        TraceContext.putIfPresent("xrayTraceId", xrayRoot(request.getHeader(XRAY_TRACE_HEADER)));
        TraceContext.putCurrentSpan();
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
            int status = response.getStatus();
            MDC.put("status", Integer.toString(status));
            MDC.put("durationMs", Long.toString(durationMs));
            TraceContext.putCurrentSpan();

            if (status >= 500) {
                log.warn("request completed with server error method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMs);
            } else if (status < 400 && durationMs >= slowRequestThresholdMs) {
                log.warn("slow request method={} path={} status={} durationMs={} thresholdMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMs, slowRequestThresholdMs);
            }

            MDC.clear();
        }
    }

    private static String requestId(HttpServletRequest request) {
        String existing = request.getHeader(REQUEST_ID_HEADER);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return UUID.randomUUID().toString();
    }

    private static String xrayRoot(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        for (String part : header.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("Root=")) {
                return trimmed.substring("Root=".length());
            }
        }
        return header;
    }
}
