package com.deva.learn.job;

public record AuthIntrospectionResponse(
        boolean active,
        String subject,
        String expiresAt) {
}
