package com.deva.modules.authfilter.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.deva.modules.authfilter.auth.AuthenticatedUser;

@RestController
public class AccountController {
    @GetMapping("/api/me")
    Map<String, Object> me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return Map.of(
                "subject", user.subject(),
                "scopes", user.scopes(),
                "expiresAt", user.expiresAt().toString());
    }

    @PostMapping("/api/orders")
    Map<String, Object> createOrder(
            Authentication authentication,
            @RequestBody CreateOrderRequest request) {

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return Map.of(
                "orderId", "ord-" + Instant.now().toEpochMilli(),
                "subject", user.subject(),
                "sku", request.sku(),
                "quantity", request.quantity(),
                "status", "ACCEPTED");
    }

    public record CreateOrderRequest(String sku, int quantity) {
    }
}

