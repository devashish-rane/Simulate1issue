package com.deva.modules.authfilter.error;

import java.util.Map;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, Object> details) {
}

