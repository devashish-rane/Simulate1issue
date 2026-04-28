package com.deva.auth.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.deva.auth.config.AppProperties;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_SK = "METADATA";

    private final DynamoDbClient dynamoDbClient;
    private final AppProperties properties;

    AuthController(DynamoDbClient dynamoDbClient, AppProperties properties) {
        this.dynamoDbClient = dynamoDbClient;
        this.properties = properties;
    }

    @PostMapping("/auth/token")
    TokenResponse token(@RequestBody TokenRequest request) {
        if (request == null || request.clientId() == null || request.clientSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "client credentials are required");
        }
        if (!"demo-client".equals(request.clientId()) || !"demo-secret".equals(request.clientSecret())) {
            log.warn("token rejected clientId={}", request.clientId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid client credentials");
        }

        String token = newToken();
        Instant expiresAt = Instant.now().plusSeconds(properties.tokenTtlSeconds());
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(properties.tableName())
                .item(tokenItem(token, request.clientId(), expiresAt))
                .build());

        log.info("token issued subject={} expiresAt={}", request.clientId(), expiresAt);
        return new TokenResponse(token, "Bearer", properties.tokenTtlSeconds(), expiresAt.toString());
    }

    @PostMapping("/auth/introspect")
    IntrospectionResponse introspect(@RequestBody IntrospectionRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            return IntrospectionResponse.inactive();
        }

        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(properties.tableName())
                .key(Map.of(
                        "pk", stringValue(pk(request.token())),
                        "sk", stringValue(TOKEN_SK)))
                .build()).item();

        if (item == null || item.isEmpty()) {
            return IntrospectionResponse.inactive();
        }

        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(item.get("expiresAtEpochSeconds").n()));
        if (!expiresAt.isAfter(Instant.now())) {
            return IntrospectionResponse.inactive();
        }

        return new IntrospectionResponse(true, item.get("subject").s(), expiresAt.toString());
    }

    private Map<String, AttributeValue> tokenItem(String token, String subject, Instant expiresAt) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", stringValue(pk(token)));
        item.put("sk", stringValue(TOKEN_SK));
        item.put("entityType", stringValue("TOKEN"));
        item.put("subject", stringValue(subject));
        item.put("createdAt", stringValue(Instant.now().toString()));
        item.put("expiresAt", stringValue(expiresAt.toString()));
        item.put("expiresAtEpochSeconds", AttributeValue.builder()
                .n(Long.toString(expiresAt.getEpochSecond()))
                .build());
        return item;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String pk(String token) {
        return "TOKEN#" + token;
    }

    private static AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    record TokenRequest(String clientId, String clientSecret) {
    }

    record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String expiresAt) {
    }

    record IntrospectionRequest(String token) {
    }

    record IntrospectionResponse(boolean active, String subject, String expiresAt) {
        static IntrospectionResponse inactive() {
            return new IntrospectionResponse(false, null, null);
        }
    }
}
