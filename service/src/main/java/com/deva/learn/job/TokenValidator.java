package com.deva.learn.job;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.deva.learn.config.AppProperties;
import com.deva.learn.exception.DependencyUnavailableException;
import com.deva.learn.exception.MissingTokenException;
import com.deva.learn.exception.UnauthorizedTokenException;

@Component
class TokenValidator {
    private final RestClient authClient;

    TokenValidator(AppProperties properties, RestClient.Builder restClientBuilder) {
        this.authClient = restClientBuilder
                .baseUrl(properties.auth().baseUrl())
                .build();
    }

    String validate(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        AuthIntrospectionResponse response;
        try {
            response = authClient.post()
                    .uri("/auth/introspect")
                    .body(Map.of("token", token))
                    .retrieve()
                    .body(AuthIntrospectionResponse.class);
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException("auth-token-provider", exception);
        }

        if (response == null || !response.active()) {
            throw new UnauthorizedTokenException();
        }
        return response.subject();
    }

    private static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new MissingTokenException();
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new MissingTokenException();
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            throw new MissingTokenException();
        }
        return token;
    }
}
