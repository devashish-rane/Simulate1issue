package com.deva.learn.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedTokenException extends ApiException {
    public UnauthorizedTokenException() {
        super(HttpStatus.UNAUTHORIZED,
                "INVALID_TOKEN",
                "Token is not active");
    }
}
