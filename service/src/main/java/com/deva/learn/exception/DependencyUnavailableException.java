package com.deva.learn.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class DependencyUnavailableException extends ApiException {
    public DependencyUnavailableException(String dependency, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "DEPENDENCY_UNAVAILABLE",
                dependency + " is unavailable",
                Map.of("dependency", dependency));
        initCause(cause);
    }
}
