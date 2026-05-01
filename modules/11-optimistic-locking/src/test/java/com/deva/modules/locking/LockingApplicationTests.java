package com.deva.modules.locking;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class LockingApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void updateWithExpectedVersionSucceedsThenStaleVersionConflicts() throws Exception {
        mockMvc.perform(get("/api/jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", equalTo(1)));
        mockMvc.perform(patch("/api/jobs/job-1?status=PROCESSING&expectedVersion=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", equalTo(2)));
        mockMvc.perform(patch("/api/jobs/job-1?status=SUCCEEDED&expectedVersion=1"))
                .andExpect(status().isConflict());
    }
}

