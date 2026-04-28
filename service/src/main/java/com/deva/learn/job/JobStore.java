package com.deva.learn.job;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.deva.learn.config.AppProperties;
import com.deva.learn.exception.JobNotFoundException;
import com.deva.learn.exception.JobSubmissionException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Component
class JobStore {
    private static final String JOB_SK = "METADATA";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    JobStore(DynamoDbClient dynamoDbClient, AppProperties properties) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = properties.jobs().tableName();
    }

    void create(JobMessage message, String createdAt) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", stringValue(pk(message.jobId())));
        item.put("sk", stringValue(JOB_SK));
        item.put("entityType", stringValue("JOB"));
        item.put("jobId", stringValue(message.jobId()));
        item.put("subject", stringValue(message.subject()));
        item.put("jobType", stringValue(message.jobType()));
        item.put("status", stringValue("PENDING"));
        item.put("slow", boolValue(message.slow()));
        item.put("fail", boolValue(message.fail()));
        item.put("poison", boolValue(message.poison()));
        item.put("durationMs", numberValue(message.durationMs()));
        item.put("createdAt", stringValue(createdAt));
        item.put("updatedAt", stringValue(createdAt));
        item.put("expiresAtEpochSeconds", numberValue(Instant.now().plusSeconds(86_400).getEpochSecond()));

        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build());
        } catch (RuntimeException exception) {
            throw new JobSubmissionException(exception);
        }
    }

    JobResponse get(String jobId) {
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "pk", stringValue(pk(jobId)),
                        "sk", stringValue(JOB_SK)))
                .build()).item();

        if (item == null || item.isEmpty()) {
            throw new JobNotFoundException(jobId);
        }

        return new JobResponse(
                string(item, "jobId"),
                string(item, "status"),
                string(item, "subject"),
                string(item, "jobType"),
                bool(item, "slow"),
                bool(item, "fail"),
                bool(item, "poison"),
                integer(item, "durationMs", 0),
                string(item, "createdAt"),
                string(item, "updatedAt"),
                optionalInteger(item, "attempt"),
                optionalString(item, "lastError"));
    }

    private static String pk(String jobId) {
        return "JOB#" + jobId;
    }

    private static AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue numberValue(long value) {
        return AttributeValue.builder().n(Long.toString(value)).build();
    }

    private static AttributeValue boolValue(boolean value) {
        return AttributeValue.builder().bool(value).build();
    }

    private static String string(Map<String, AttributeValue> item, String name) {
        AttributeValue value = item.get(name);
        return value == null ? "" : value.s();
    }

    private static boolean bool(Map<String, AttributeValue> item, String name) {
        AttributeValue value = item.get(name);
        return value != null && Boolean.TRUE.equals(value.bool());
    }

    private static int integer(Map<String, AttributeValue> item, String name, int fallback) {
        AttributeValue value = item.get(name);
        if (value == null || value.n() == null) {
            return fallback;
        }
        return Integer.parseInt(value.n());
    }

    private static Integer optionalInteger(Map<String, AttributeValue> item, String name) {
        AttributeValue value = item.get(name);
        if (value == null || value.n() == null) {
            return null;
        }
        return Integer.valueOf(value.n());
    }

    private static String optionalString(Map<String, AttributeValue> item, String name) {
        AttributeValue value = item.get(name);
        return value == null ? null : value.s();
    }
}
