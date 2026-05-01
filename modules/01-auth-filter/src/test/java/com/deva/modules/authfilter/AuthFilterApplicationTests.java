package com.deva.modules.authfilter;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=INFO",
        "logging.level.org.springframework=INFO"
})
@AutoConfigureMockMvc
class AuthFilterApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("ok")));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("AUTH_MISSING")));
    }

    @Test
    void protectedEndpointRejectsMalformedHeader() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("AUTH_MALFORMED")));
    }

    @Test
    void protectedEndpointRejectsInactiveToken() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("AUTH_INACTIVE")));
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer demo-user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", equalTo("customer-100")))
                .andExpect(jsonPath("$.scopes", hasItem("profile:read")));
    }

    @Test
    void protectedEndpointReportsAuthProviderFailure() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer provider-down-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", equalTo("AUTH_PROVIDER_UNAVAILABLE")));
    }
}
