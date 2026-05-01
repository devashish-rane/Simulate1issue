package com.deva.modules.nplusone;

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
class NPlusOneApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void riskyEndpointDocumentsNPlusOneMode() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].queryMode", equalTo("n-plus-one-risk")));
    }

    @Test
    void fixedEndpointUsesEntityGraphMode() throws Exception {
        mockMvc.perform(get("/api/customers?fixed=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].queryMode", equalTo("entity-graph")));
    }
}

