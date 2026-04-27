package com.deva.learn.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class SessionRejectedException extends ApiException {
    public SessionRejectedException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "SESSION_REJECTED",
                "Session token is missing, expired, or invalid",
                Map.of("tokenStatus", "missing_or_invalid"));
    }
}
