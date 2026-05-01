package com.deva.modules.tenantcontext;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SpringBootApplication
public class TenantContextApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantContextApplication.class, args);
    }
}

final class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();

    private TenantContext() {
    }

    static void set(String tenantId, String userId) {
        TENANT.set(tenantId);
        USER.set(userId);
    }

    static String tenantId() {
        return TENANT.get();
    }

    static String userId() {
        return USER.get();
    }

    static void clear() {
        TENANT.remove();
        USER.remove();
    }
}

@Component
class TenantContextFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    TenantContextFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");
        String memberships = request.getHeader("X-User-Tenants");

        if (tenantId == null || tenantId.isBlank()) {
            reject(response, request, HttpStatus.BAD_REQUEST, "TENANT_MISSING", "X-Tenant-Id is required");
            return;
        }
        if (!tenantId.matches("tenant-[a-z0-9-]+")) {
            reject(response, request, HttpStatus.BAD_REQUEST, "TENANT_INVALID", "Tenant id format is invalid");
            return;
        }
        if (userId == null || memberships == null || !tenantMemberships(memberships).contains(tenantId)) {
            reject(response, request, HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", "User is not a member of tenant");
            return;
        }

        TenantContext.set(tenantId, userId);
        MDC.put("tenantId", tenantId);
        MDC.put("userId", userId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }

    private Set<String> tenantMemberships(String header) {
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "code", code,
                "message", message,
                "path", request.getRequestURI()));
    }
}

@RestController
class TenantController {
    @GetMapping("/public/ping")
    Map<String, Object> ping() {
        return Map.of("status", "ok");
    }

    @GetMapping("/api/current-tenant")
    Map<String, Object> currentTenant() {
        return Map.of(
                "tenantId", TenantContext.tenantId(),
                "userId", TenantContext.userId());
    }
}

