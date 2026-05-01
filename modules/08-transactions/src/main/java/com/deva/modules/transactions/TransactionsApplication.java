package com.deva.modules.transactions;

import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@SpringBootApplication
public class TransactionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionsApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(AccountRepository repository) {
        return args -> {
            repository.save(new Account("wallet-a", 1_000));
            repository.save(new Account("wallet-b", 500));
        };
    }
}

@Entity
class Account {
    @Id
    private String id;
    private int balanceCents;

    protected Account() {
    }

    Account(String id, int balanceCents) {
        this.id = id;
        this.balanceCents = balanceCents;
    }

    String id() {
        return id;
    }

    int balanceCents() {
        return balanceCents;
    }

    void debit(int amountCents) {
        if (balanceCents < amountCents) {
            throw new InsufficientFundsException();
        }
        balanceCents -= amountCents;
    }

    void credit(int amountCents) {
        balanceCents += amountCents;
    }
}

interface AccountRepository extends JpaRepository<Account, String> {
}

@Service
class TransferService {
    private final AccountRepository repository;

    TransferService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    void transfer(String fromId, String toId, int amountCents, boolean failAfterDebit) {
        Account from = repository.findById(fromId).orElseThrow(AccountNotFoundException::new);
        Account to = repository.findById(toId).orElseThrow(AccountNotFoundException::new);
        from.debit(amountCents);
        if (failAfterDebit) {
            throw new SimulatedMidTransactionFailure();
        }
        to.credit(amountCents);
    }
}

@RestController
class TransferController {
    private final AccountRepository repository;
    private final TransferService transferService;

    TransferController(AccountRepository repository, TransferService transferService) {
        this.repository = repository;
        this.transferService = transferService;
    }

    @GetMapping("/api/accounts")
    List<Map<String, Object>> accounts() {
        return repository.findAll().stream()
                .map(account -> Map.<String, Object>of("id", account.id(), "balanceCents", account.balanceCents()))
                .toList();
    }

    @PostMapping("/api/transfers")
    Map<String, Object> transfer(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam int amountCents,
            @RequestParam(defaultValue = "false") boolean failAfterDebit) {
        transferService.transfer(from, to, amountCents, failAfterDebit);
        return Map.of("status", "COMMITTED");
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class AccountNotFoundException extends RuntimeException {
}

@ResponseStatus(HttpStatus.CONFLICT)
class InsufficientFundsException extends RuntimeException {
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class SimulatedMidTransactionFailure extends RuntimeException {
}

