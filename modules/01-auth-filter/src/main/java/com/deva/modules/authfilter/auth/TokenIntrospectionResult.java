package com.deva.modules.authfilter.auth;

import java.time.Instant;
import java.util.List;

public record TokenIntrospectionResult(
        boolean active,
        String subject,
        List<String> scopes,
        Instant expiresAt,
        String reason) {

    public static TokenIntrospectionResult active(String subject, List<String> scopes, Instant expiresAt) {
        return new TokenIntrospectionResult(true, subject, List.copyOf(scopes), expiresAt, "active");
    }

    public static TokenIntrospectionResult inactive(String reason) {
        return new TokenIntrospectionResult(false, null, List.of(), null, reason);
    }
}

