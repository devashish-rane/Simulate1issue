package com.deva.modules.queueretries;

import static org.hamcrest.Matchers.equalTo;
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
class QueueRetriesApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void normalMessageIsProcessedAndRemoved() throws Exception {
        mockMvc.perform(post("/api/messages?mode=normal")).andExpect(status().isOk());
        mockMvc.perform(post("/api/worker/process-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed", equalTo(true)));
    }

    @Test
    void poisonMessageEventuallyMovesToDlq() throws Exception {
        mockMvc.perform(post("/api/messages?mode=poison")).andExpect(status().isOk());
        mockMvc.perform(post("/api/worker/process-one")).andExpect(status().isOk());
        mockMvc.perform(post("/api/worker/process-one")).andExpect(status().isOk());
        mockMvc.perform(post("/api/worker/process-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", equalTo("sent-to-dlq")));
        mockMvc.perform(get("/api/queue/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dlq", equalTo(1)));
    }
}

