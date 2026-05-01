package com.deva.modules.authfilter.auth;

public class AuthProviderUnavailableException extends RuntimeException {
    public AuthProviderUnavailableException(String message) {
        super(message);
    }
}

