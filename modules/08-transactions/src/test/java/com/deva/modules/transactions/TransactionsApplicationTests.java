package com.deva.modules.transactions;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class TransactionsApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository repository;

    @Test
    void successfulTransferCommitsBothUpdates() throws Exception {
        mockMvc.perform(post("/api/transfers?from=wallet-a&to=wallet-b&amountCents=100"))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(repository.findById("wallet-a").orElseThrow().balanceCents())
                .isEqualTo(900);
        org.assertj.core.api.Assertions.assertThat(repository.findById("wallet-b").orElseThrow().balanceCents())
                .isEqualTo(600);
    }

    @Test
    void failureAfterDebitRollsBackEntireTransaction() throws Exception {
        int beforeA = repository.findById("wallet-a").orElseThrow().balanceCents();
        int beforeB = repository.findById("wallet-b").orElseThrow().balanceCents();

        mockMvc.perform(post("/api/transfers?from=wallet-a&to=wallet-b&amountCents=100&failAfterDebit=true"))
                .andExpect(status().isInternalServerError());

        org.assertj.core.api.Assertions.assertThat(repository.findById("wallet-a").orElseThrow().balanceCents())
                .isEqualTo(beforeA);
        org.assertj.core.api.Assertions.assertThat(repository.findById("wallet-b").orElseThrow().balanceCents())
                .isEqualTo(beforeB);
    }
}

