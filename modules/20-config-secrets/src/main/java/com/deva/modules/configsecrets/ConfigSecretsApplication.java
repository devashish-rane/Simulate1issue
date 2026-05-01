package com.deva.modules.configsecrets;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

@SpringBootApplication
@EnableConfigurationProperties(LabProperties.class)
public class ConfigSecretsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigSecretsApplication.class, args);
    }
}

@Validated
@ConfigurationProperties(prefix = "lab")
record LabProperties(
        @NotBlank String requiredRegion,
        @NotBlank String secretToken,
        boolean optionalFeatureEnabled) {
}

@RestController
class ConfigController {
    private final LabProperties properties;

    ConfigController(LabProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/config/status")
    Map<String, Object> status() {
        return Map.of(
                "requiredRegion", properties.requiredRegion(),
                "secretLoaded", true,
                "secretPreview", mask(properties.secretToken()),
                "optionalFeatureEnabled", properties.optionalFeatureEnabled());
    }

    private String mask(String secret) {
        return secret.length() <= 4 ? "****" : secret.substring(0, 2) + "****" + secret.substring(secret.length() - 2);
    }
}

