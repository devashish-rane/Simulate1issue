package com.deva.modules.healthdesign;

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
class HealthDesignApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealthCanBeUpWhileBusinessHealthFails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")));
        mockMvc.perform(get("/business-health?dependency=down"))
                .andExpect(status().isServiceUnavailable());
    }
}

