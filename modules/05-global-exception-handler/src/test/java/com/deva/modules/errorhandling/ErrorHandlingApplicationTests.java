package com.deva.modules.errorhandling;

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
class ErrorHandlingApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessErrorReturnsStableCode() throws Exception {
        mockMvc.perform(get("/api/quote?plan=starter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_PLAN")));
    }

    @Test
    void dependencyErrorReturns503() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", equalTo("INVENTORY_UNAVAILABLE")));
    }

    @Test
    void unexpectedErrorIsMaskedFromClient() throws Exception {
        mockMvc.perform(get("/api/bug"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", equalTo("INTERNAL_ERROR")))
                .andExpect(jsonPath("$.message", equalTo("Unexpected server error")));
    }
}

