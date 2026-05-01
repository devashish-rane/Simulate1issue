package com.deva.modules.authfilter.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.deva.modules.authfilter.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(BearerTokenAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenIntrospectionClient tokenIntrospectionClient;
    private final ObjectMapper objectMapper;

    public BearerTokenAuthenticationFilter(
            TokenIntrospectionClient tokenIntrospectionClient,
            ObjectMapper objectMapper) {
        this.tokenIntrospectionClient = tokenIntrospectionClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = requestId(request);
        MDC.put("requestId", requestId);

        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || authorization.isBlank()) {
                reject(response, request, HttpStatus.UNAUTHORIZED, "AUTH_MISSING", "Bearer token is required");
                return;
            }

            if (!authorization.startsWith(BEARER_PREFIX)) {
                reject(response, request, HttpStatus.UNAUTHORIZED, "AUTH_MALFORMED",
                        "Authorization header must use Bearer scheme");
                return;
            }

            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (token.isBlank()) {
                reject(response, request, HttpStatus.UNAUTHORIZED, "AUTH_EMPTY", "Bearer token is empty");
                return;
            }

            String tokenHash = tokenFingerprint(token);
            MDC.put("tokenHash", tokenHash);

            TokenIntrospectionResult result = tokenIntrospectionClient.introspect(token);
            if (!result.active()) {
                log.warn("authentication rejected reason={} path={}", result.reason(), request.getRequestURI());
                reject(response, request, HttpStatus.UNAUTHORIZED, "AUTH_INACTIVE", "Bearer token is not active");
                return;
            }

            AuthenticatedUser principal = new AuthenticatedUser(result.subject(), result.scopes(), result.expiresAt());
            MDC.put("subject", principal.subject());

            List<SimpleGrantedAuthority> authorities = principal.scopes().stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, tokenHash, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("authentication accepted path={} scopes={}", request.getRequestURI(), principal.scopes());
            filterChain.doFilter(request, response);
        } catch (AuthProviderUnavailableException exception) {
            log.warn("authentication provider unavailable path={} message={}",
                    request.getRequestURI(),
                    exception.getMessage());
            reject(response, request, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_PROVIDER_UNAVAILABLE",
                    "Authentication provider is unavailable");
        } catch (RuntimeException exception) {
            log.error("authentication failed unexpectedly path={}", request.getRequestURI(), exception);
            reject(response, request, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_FILTER_FAILURE",
                    "Authentication could not be completed");
        } finally {
            SecurityContextHolder.clearContext();
            MDC.remove("requestId");
            MDC.remove("subject");
            MDC.remove("tokenHash");
        }
    }

    private void reject(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                Map.of("method", request.getMethod()));
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static String requestId(HttpServletRequest request) {
        String gatewayRequestId = request.getHeader("x-amzn-requestid");
        if (gatewayRequestId != null && !gatewayRequestId.isBlank()) {
            return gatewayRequestId;
        }
        return UUID.randomUUID().toString();
    }

    private static String tokenFingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

