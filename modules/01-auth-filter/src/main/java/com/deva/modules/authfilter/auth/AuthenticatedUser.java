package com.deva.modules.authfilter.auth;

import java.time.Instant;
import java.util.List;

public record AuthenticatedUser(
        String subject,
        List<String> scopes,
        Instant expiresAt) {
}

