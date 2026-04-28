package com.deva.learn.exception;

import org.springframework.http.HttpStatus;

public class MissingTokenException extends ApiException {
    public MissingTokenException() {
        super(HttpStatus.UNAUTHORIZED,
                "MISSING_TOKEN",
                "Missing bearer token");
    }
}
