package com.deva.modules.authfilter.auth;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class InMemoryTokenIntrospectionClient implements TokenIntrospectionClient {
    @Override
    public TokenIntrospectionResult introspect(String token) {
        return switch (token) {
            case "demo-user-token" -> TokenIntrospectionResult.active(
                    "customer-100",
                    List.of("profile:read", "orders:create"),
                    Instant.now().plusSeconds(900));
            case "demo-admin-token" -> TokenIntrospectionResult.active(
                    "admin-1",
                    List.of("profile:read", "orders:create", "ops:read"),
                    Instant.now().plusSeconds(900));
            case "slow-valid-token" -> slowToken();
            case "provider-down-token" -> throw new AuthProviderUnavailableException("simulated introspection outage");
            case "expired-token" -> TokenIntrospectionResult.inactive("expired");
            default -> TokenIntrospectionResult.inactive("unknown_token");
        };
    }

    private static TokenIntrospectionResult slowToken() {
        try {
            Thread.sleep(1_200);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthProviderUnavailableException("token introspection interrupted");
        }

        return TokenIntrospectionResult.active(
                "slow-customer",
                List.of("profile:read"),
                Instant.now().plusSeconds(900));
    }
}

