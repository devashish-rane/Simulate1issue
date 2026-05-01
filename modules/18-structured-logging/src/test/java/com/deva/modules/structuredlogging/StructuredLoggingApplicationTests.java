package com.deva.modules.structuredlogging;

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
class StructuredLoggingApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void correlationFieldsAreAvailableToRequest() throws Exception {
        mockMvc.perform(get("/api/log-demo")
                        .header("X-Trace-Id", "trace-123")
                        .header("X-Span-Id", "span-456")
                        .header("X-Tenant-Id", "tenant-alpha")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId", equalTo("trace-123")))
                .andExpect(jsonPath("$.spanId", equalTo("span-456")))
                .andExpect(jsonPath("$.tenantId", equalTo("tenant-alpha")));
    }
}

