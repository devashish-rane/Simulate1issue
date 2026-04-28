package com.deva.learn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Auth auth, Jobs jobs) {
    public record Auth(String baseUrl) {
    }

    public record Jobs(String tableName, String queueUrl) {
    }
}
