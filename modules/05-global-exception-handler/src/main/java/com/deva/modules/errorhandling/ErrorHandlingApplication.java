package com.deva.modules.errorhandling;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

@SpringBootApplication
public class ErrorHandlingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErrorHandlingApplication.class, args);
    }
}

record ApiError(String timestamp, int status, String code, String message, String path, Map<String, Object> details) {
}

class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String safeMessage;
    private final Map<String, Object> details;

    ApiException(HttpStatus status, String code, String safeMessage, Map<String, Object> details) {
        super(safeMessage);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
        this.details = details;
    }

    HttpStatus status() {
        return status;
    }

    String code() {
        return code;
    }

    String safeMessage() {
        return safeMessage;
    }

    Map<String, Object> details() {
        return details;
    }
}

@RestController
class FailureController {
    @GetMapping("/api/quote")
    Map<String, Object> quote(@RequestParam(defaultValue = "starter") String plan) {
        if (!"standard".equals(plan) && !"premium".equals(plan)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PLAN", "Plan is not supported", Map.of("plan", plan));
        }
        return Map.of("plan", plan, "status", "quoted");
    }

    @GetMapping("/api/inventory")
    Map<String, Object> inventory() {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_UNAVAILABLE",
                "Inventory service is temporarily unavailable", Map.of("dependency", "inventory"));
    }

    @GetMapping("/api/bug")
    Map<String, Object> bug() {
        throw new IllegalStateException("simulated unhandled bug");
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception, ServletWebRequest request) {
        log.warn("request failed code={} status={} path={} details={}",
                exception.code(), exception.status().value(), request.getRequest().getRequestURI(), exception.details());
        return ResponseEntity.status(exception.status()).body(new ApiError(
                Instant.now().toString(),
                exception.status().value(),
                exception.code(),
                exception.safeMessage(),
                request.getRequest().getRequestURI(),
                exception.details()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, ServletWebRequest request) {
        log.error("request failed unexpectedly path={}", request.getRequest().getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                Instant.now().toString(),
                500,
                "INTERNAL_ERROR",
                "Unexpected server error",
                request.getRequest().getRequestURI(),
                Map.of()));
    }
}

