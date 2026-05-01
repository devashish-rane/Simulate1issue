package com.deva.modules.pagination;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
class PaginationApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void offsetPaginationCapsLimit() throws Exception {
        mockMvc.perform(get("/api/jobs/offset?page=0&limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(50)));
    }

    @Test
    void cursorPaginationReturnsNextCursor() throws Exception {
        mockMvc.perform(get("/api/jobs/cursor?limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.nextCursor", equalTo("job-002")));
    }
}

