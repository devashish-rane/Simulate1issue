package com.deva.modules.rbac;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class RbacApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void userCanReadOwnJobs() throws Exception {
        mockMvc.perform(get("/api/jobs/self").header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", equalTo("user-100")));
    }

    @Test
    void userCannotReadAllJobs() throws Exception {
        mockMvc.perform(get("/api/jobs/all").header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportCanReadAllJobsButNotOpsReport() throws Exception {
        mockMvc.perform(get("/api/jobs/all").header(HttpHeaders.AUTHORIZATION, "Bearer support-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ops/report").header(HttpHeaders.AUTHORIZATION, "Bearer support-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadOpsReport() throws Exception {
        mockMvc.perform(get("/api/ops/report").header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk());
    }
}

