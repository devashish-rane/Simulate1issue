package com.deva.modules.tenantcontext;

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
class TenantContextApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingTenantIsRejected() throws Exception {
        mockMvc.perform(get("/api/current-tenant"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("TENANT_MISSING")));
    }

    @Test
    void nonMemberTenantIsRejected() throws Exception {
        mockMvc.perform(get("/api/current-tenant")
                        .header("X-Tenant-Id", "tenant-beta")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Tenants", "tenant-alpha"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo("TENANT_ACCESS_DENIED")));
    }

    @Test
    void validTenantContextIsAvailableToController() throws Exception {
        mockMvc.perform(get("/api/current-tenant")
                        .header("X-Tenant-Id", "tenant-alpha")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Tenants", "tenant-alpha,tenant-beta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", equalTo("tenant-alpha")))
                .andExpect(jsonPath("$.userId", equalTo("user-1")));
    }
}

