package com.deva.modules.resilience;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ResilienceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResilienceApplication.class, args);
    }
}

@Service
class ProfileDependencyClient {
    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    Map<String, Object> fetch(String mode) {
        if (consecutiveFailures.get() >= 2) {
            throw new CircuitOpenException();
        }
        long start = System.nanoTime();
        try {
            if ("down".equals(mode)) {
                consecutiveFailures.incrementAndGet();
                throw new DependencyUnavailableException();
            }
            if ("slow".equals(mode)) {
                Thread.sleep(350);
            }
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            if (tookMs > 200) {
                consecutiveFailures.incrementAndGet();
                throw new DependencyTimeoutException();
            }
            consecutiveFailures.set(0);
            return Map.of("dependency", "profile-store", "status", "ok", "latencyMs", tookMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DependencyUnavailableException();
        }
    }
}

@RestController
class ResilienceController {
    private final ProfileDependencyClient client;

    ResilienceController(ProfileDependencyClient client) {
        this.client = client;
    }

    @GetMapping("/api/profile")
    Map<String, Object> profile(@RequestParam(defaultValue = "ok") String mode) {
        return Map.of("profile", client.fetch(mode), "resilience", "timeout-plus-circuit-breaker");
    }
}

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
class DependencyTimeoutException extends RuntimeException {
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class DependencyUnavailableException extends RuntimeException {
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class CircuitOpenException extends RuntimeException {
}

