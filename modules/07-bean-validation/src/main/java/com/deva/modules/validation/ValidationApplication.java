package com.deva.modules.validation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@SpringBootApplication
public class ValidationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ValidationApplication.class, args);
    }
}

record CreateShipmentRequest(
        @NotBlank String orderId,
        @Pattern(regexp = "tenant-[a-z0-9-]+") String tenantId,
        @Min(1) @Max(50) int packageCount) {
}

@RestController
class ShipmentController {
    @PostMapping("/api/shipments")
    Map<String, Object> create(@Valid @RequestBody CreateShipmentRequest request) {
        return Map.of(
                "shipmentId", "ship-" + Math.abs(request.orderId().hashCode()),
                "tenantId", request.tenantId(),
                "packageCount", request.packageCount(),
                "status", "CREATED");
    }
}

@RestControllerAdvice
class ValidationExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handle(MethodArgumentNotValidException exception) {
        List<Map<String, Object>> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "code", "VALIDATION_FAILED",
                "message", "Request validation failed",
                "fields", fields));
    }

    private Map<String, Object> fieldError(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage());
    }
}

