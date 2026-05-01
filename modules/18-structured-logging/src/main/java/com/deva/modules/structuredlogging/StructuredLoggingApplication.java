package com.deva.modules.structuredlogging;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SpringBootApplication
public class StructuredLoggingApplication {
    public static void main(String[] args) {
        SpringApplication.run(StructuredLoggingApplication.class, args);
    }
}

@Component
class CorrelationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        MDC.put("traceId", headerOr(request, "X-Trace-Id", UUID.randomUUID().toString()));
        MDC.put("spanId", headerOr(request, "X-Span-Id", UUID.randomUUID().toString().substring(0, 8)));
        MDC.put("tenantId", headerOr(request, "X-Tenant-Id", "unknown"));
        MDC.put("userId", headerOr(request, "X-User-Id", "anonymous"));
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String headerOr(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? fallback : value;
    }
}

@RestController
class LoggingController {
    private static final Logger log = LoggerFactory.getLogger(LoggingController.class);

    @GetMapping("/api/log-demo")
    Map<String, Object> demo() {
        log.info("business event completed event=log-demo");
        return Map.of(
                "traceId", MDC.get("traceId"),
                "spanId", MDC.get("spanId"),
                "tenantId", MDC.get("tenantId"),
                "userId", MDC.get("userId"));
    }
}

