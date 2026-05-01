package com.deva.modules.metrics;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@SpringBootApplication
public class MetricsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetricsApplication.class, args);
    }
}

@RestController
class MetricsJobController {
    private final AtomicInteger sequence = new AtomicInteger();
    private final Counter createdCounter;
    private final Timer creationTimer;

    MetricsJobController(MeterRegistry registry) {
        this.createdCounter = Counter.builder("lab.jobs.created")
                .description("Jobs accepted by the API")
                .tag("type", "scan")
                .register(registry);
        this.creationTimer = Timer.builder("lab.jobs.create.latency")
                .description("Job creation latency")
                .register(registry);
    }

    @PostMapping("/api/jobs")
    Map<String, Object> create() {
        return creationTimer.record(() -> {
            createdCounter.increment();
            return Map.of("jobId", "job-" + sequence.incrementAndGet(), "status", "ACCEPTED");
        });
    }
}

