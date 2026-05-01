package com.deva.modules.tenantisolation;

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
class TenantIsolationApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void tenantCanReadOwnJob() throws Exception {
        mockMvc.perform(get("/api/jobs/job-100").header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", equalTo("tenant-alpha")));
    }

    @Test
    void tenantCannotReadOtherTenantJob() throws Exception {
        mockMvc.perform(get("/api/jobs/job-200").header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isNotFound());
    }

    @Test
    void supportLookupRequiresSupportRole() throws Exception {
        mockMvc.perform(get("/api/support/jobs/job-200").header("X-Support-Role", "SUPPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access", equalTo("audited-support")));
        mockMvc.perform(get("/api/support/jobs/job-200").header("X-Support-Role", "USER"))
                .andExpect(status().isNotFound());
    }
}

