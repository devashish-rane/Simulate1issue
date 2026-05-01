package com.deva.modules.configsecrets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
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
class ConfigSecretsApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void configStatusMasksSecret() throws Exception {
        mockMvc.perform(get("/api/config/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredRegion", equalTo("us-east-1")))
                .andExpect(jsonPath("$.secretLoaded", equalTo(true)))
                .andExpect(jsonPath("$.secretPreview", not(equalTo("local-dev-secret"))));
    }
}

