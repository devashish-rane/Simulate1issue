package com.deva.modules.authfilter.auth;

public interface TokenIntrospectionClient {
    TokenIntrospectionResult introspect(String token);
}

