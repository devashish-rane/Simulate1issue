package com.deva.learn.exception;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        log.warn(
                "request failed code={} status={} path={} message={} details={}",
                exception.getCode(),
                exception.getStatus().value(),
                request.getRequestURI(),
                exception.getMessage(),
                exception.getDetails());

        return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(
                Instant.now().toString(),
                exception.getStatus().value(),
                exception.getStatus().getReasonPhrase(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        log.error("unexpected request failure path={}", request.getRequestURI(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                Instant.now().toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "UNEXPECTED_ERROR",
                "Unexpected server error",
                request.getRequestURI(),
                Map.of()));
    }
}
