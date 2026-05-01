package com.deva.modules.resilience;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class ResilienceApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void okDependencyReturnsProfile() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.status", equalTo("ok")));
    }

    @Test
    void slowDependencyTimesOutAndRepeatedFailuresOpenCircuit() throws Exception {
        mockMvc.perform(get("/api/profile?mode=slow")).andExpect(status().isGatewayTimeout());
        mockMvc.perform(get("/api/profile?mode=slow")).andExpect(status().isGatewayTimeout());
        mockMvc.perform(get("/api/profile")).andExpect(status().isServiceUnavailable());
    }
}

