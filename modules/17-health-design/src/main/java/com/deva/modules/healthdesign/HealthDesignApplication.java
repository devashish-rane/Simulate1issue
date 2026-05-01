package com.deva.modules.healthdesign;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class HealthDesignApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthDesignApplication.class, args);
    }
}

@RestController
class BusinessHealthController {
    @GetMapping("/business-health")
    Map<String, Object> businessHealth(@RequestParam(defaultValue = "ok") String dependency) {
        if ("down".equals(dependency)) {
            throw new BusinessDependencyDownException();
        }
        return Map.of("status", "UP", "lesson", "business canary checks user-critical behavior");
    }

    @GetMapping("/cheap-readiness")
    Map<String, Object> readiness() {
        return Map.of("status", "READY", "lesson", "load balancer check should be cheap");
    }
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class BusinessDependencyDownException extends RuntimeException {
}

