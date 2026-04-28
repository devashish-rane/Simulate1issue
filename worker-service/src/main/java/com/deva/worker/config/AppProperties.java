package com.deva.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String tableName, String queueUrl, long pollDelayMs) {
}
