package com.deva.modules.rbac;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
public class RbacApplication {
    public static void main(String[] args) {
        SpringApplication.run(RbacApplication.class, args);
    }
}

record DemoPrincipal(String subject, String role, Set<String> permissions) {
}

@Component
class DemoTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(DemoTokenFilter.class);
    private static final String PREFIX = "Bearer ";
    private final ObjectMapper objectMapper;

    DemoTokenFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            reject(response, request, HttpStatus.UNAUTHORIZED, "AUTH_MISSING", "Bearer token required");
            return;
        }

        DemoPrincipal principal = principalFor(header.substring(PREFIX.length()).trim());
        if (principal == null) {
            reject(response, request, HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Token is not recognized");
            return;
        }

        List<SimpleGrantedAuthority> authorities = principal.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "redacted", authorities));
        log.info("authenticated subject={} role={} permissions={}", principal.subject(), principal.role(), principal.permissions());
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private DemoPrincipal principalFor(String token) {
        return switch (token) {
            case "user-token" -> new DemoPrincipal("user-100", "USER", Set.of("job:create", "job:read:self"));
            case "support-token" -> new DemoPrincipal("support-7", "SUPPORT", Set.of("job:read:self", "job:read:any"));
            case "admin-token" -> new DemoPrincipal("admin-1", "ADMIN", Set.of("job:create", "job:read:self", "job:read:any", "ops:read"));
            default -> null;
        };
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

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, DemoTokenFilter filter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/public/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

@RestController
class RbacController {
    @GetMapping("/public/ping")
    Map<String, Object> ping() {
        return Map.of("status", "ok");
    }

    @GetMapping("/api/jobs/self")
    @PreAuthorize("hasAuthority('job:read:self')")
    Map<String, Object> myJobs() {
        DemoPrincipal principal = (DemoPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Map.of("subject", principal.subject(), "jobs", List.of("job-owned-by-" + principal.subject()));
    }

    @GetMapping("/api/jobs/all")
    @PreAuthorize("hasAuthority('job:read:any')")
    Map<String, Object> allJobs() {
        return Map.of("jobs", List.of("job-a", "job-b"), "scope", "all-tenants");
    }

    @GetMapping("/api/ops/report")
    @PreAuthorize("hasAuthority('ops:read')")
    Map<String, Object> opsReport() {
        return Map.of("status", "green", "generatedAt", Instant.now().toString());
    }
}

