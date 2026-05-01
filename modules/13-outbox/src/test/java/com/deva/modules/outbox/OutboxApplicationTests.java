package com.deva.modules.outbox;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class OutboxApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void creatingJobPersistsOutboxEventAndPublisherMarksItSent() throws Exception {
        mockMvc.perform(post("/api/jobs?type=scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outbox", equalTo("event-persisted")));
        mockMvc.perform(get("/api/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(post("/api/outbox/publish-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published", equalTo(true)));
        mockMvc.perform(get("/api/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

