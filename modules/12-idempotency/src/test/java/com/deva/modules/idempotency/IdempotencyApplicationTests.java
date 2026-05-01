package com.deva.modules.idempotency;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class IdempotencyApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void sameKeyAndSamePayloadReturnsSameJob() throws Exception {
        String body = "{\"type\":\"scan\",\"payload\":\"abc\"}";
        mockMvc.perform(post("/api/jobs").header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed", equalTo(false)));
        mockMvc.perform(post("/api/jobs").header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed", equalTo(true)));
    }

    @Test
    void sameKeyDifferentPayloadConflicts() throws Exception {
        mockMvc.perform(post("/api/jobs").header("Idempotency-Key", "key-2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"scan\",\"payload\":\"abc\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/jobs").header("Idempotency-Key", "key-2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"scan\",\"payload\":\"xyz\"}"))
                .andExpect(status().isConflict());
    }
}

