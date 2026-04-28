package com.deva.learn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsClientConfig {
    private static final String DEFAULT_REGION = "us-east-1";

    @Bean
    DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(region())
                .build();
    }

    @Bean
    SqsClient sqsClient() {
        return SqsClient.builder()
                .region(region())
                .build();
    }

    private static Region region() {
        return Region.of(System.getenv().getOrDefault("AWS_REGION", DEFAULT_REGION));
    }
}
