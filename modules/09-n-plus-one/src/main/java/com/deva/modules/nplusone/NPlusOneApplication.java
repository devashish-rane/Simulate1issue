package com.deva.modules.nplusone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@SpringBootApplication
public class NPlusOneApplication {
    public static void main(String[] args) {
        SpringApplication.run(NPlusOneApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(CustomerRepository repository) {
        return args -> {
            for (int i = 1; i <= 3; i++) {
                Customer customer = new Customer("customer-" + i);
                customer.addJob(new CustomerJob("job-" + i + "-a"));
                customer.addJob(new CustomerJob("job-" + i + "-b"));
                repository.save(customer);
            }
        };
    }
}

@Entity
class Customer {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<CustomerJob> jobs = new ArrayList<>();

    protected Customer() {
    }

    Customer(String name) {
        this.name = name;
    }

    void addJob(CustomerJob job) {
        job.assignTo(this);
        jobs.add(job);
    }

    String name() {
        return name;
    }

    List<CustomerJob> jobs() {
        return jobs;
    }
}

@Entity
class CustomerJob {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @jakarta.persistence.ManyToOne
    private Customer customer;

    protected CustomerJob() {
    }

    CustomerJob(String name) {
        this.name = name;
    }

    void assignTo(Customer customer) {
        this.customer = customer;
    }

    String name() {
        return name;
    }
}

interface CustomerRepository extends JpaRepository<Customer, Long> {
    @EntityGraph(attributePaths = "jobs")
    List<Customer> findWithJobsBy();
}

@RestController
class CustomerController {
    private final CustomerRepository repository;

    CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/customers")
    @Transactional(readOnly = true)
    List<Map<String, Object>> customers(@RequestParam(defaultValue = "false") boolean fixed) {
        List<Customer> customers = fixed ? repository.findWithJobsBy() : repository.findAll();
        return customers.stream()
                .map(customer -> Map.<String, Object>of(
                        "name", customer.name(),
                        "jobCount", customer.jobs().size(),
                        "queryMode", fixed ? "entity-graph" : "n-plus-one-risk"))
                .toList();
    }
}
