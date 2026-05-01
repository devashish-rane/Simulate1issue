package com.deva.worker.job;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.deva.worker.config.AppProperties;
import com.deva.worker.observability.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
class JobWorker {
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final String JOB_SK = "METADATA";

    private final SqsClient sqsClient;
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final long slowMessageThresholdMs;

    JobWorker(
            SqsClient sqsClient,
            DynamoDbClient dynamoDbClient,
            ObjectMapper objectMapper,
            AppProperties properties,
            @Value("${observability.slow-message-threshold-ms:1000}") long slowMessageThresholdMs) {
        this.sqsClient = sqsClient;
        this.dynamoDbClient = dynamoDbClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.slowMessageThresholdMs = slowMessageThresholdMs;
    }

    @Scheduled(fixedDelayString = "${app.poll-delay-ms:1000}")
    void poll() {
        if (properties.queueUrl() == null || properties.queueUrl().isBlank()) {
            return;
        }

        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .visibilityTimeout(30)
                .messageAttributeNames("All")
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                .build()).messages();

        for (Message message : messages) {
            process(message);
        }
    }

    private void process(Message sqsMessage) {
        long startedAtNanos = System.nanoTime();
        JobMessage job;
        int attempt = attempt(sqsMessage);
        try {
            MDC.put("messageId", sqsMessage.messageId());
            MDC.put("attempt", Integer.toString(attempt));
            TraceContext.putCurrentSpan();
            TraceContext.putMessageAttributes(sqsMessage);

            job = objectMapper.readValue(sqsMessage.body(), JobMessage.class);
            MDC.put("jobId", job.jobId());
            update(job.jobId(), "PROCESSING", attempt, null);

            if (job.slow()) {
                Thread.sleep(Math.max(100, Math.min(job.durationMs(), 15_000)));
            }
            if (job.poison()) {
                throw new IllegalStateException("poison job requested");
            }
            if (job.fail() && attempt < 3) {
                throw new IllegalStateException("controlled retry failure requested");
            }

            update(job.jobId(), "SUCCEEDED", attempt, null);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(properties.queueUrl())
                    .receiptHandle(sqsMessage.receiptHandle())
                    .build());
            long durationMs = elapsedMs(startedAtNanos);
            MDC.put("durationMs", Long.toString(durationMs));
            if (durationMs >= slowMessageThresholdMs) {
                log.warn("slow job completed jobId={} attempt={} durationMs={} thresholdMs={}",
                        job.jobId(), attempt, durationMs, slowMessageThresholdMs);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("worker interrupted", exception);
        } catch (Exception exception) {
            handleFailure(sqsMessage, attempt, exception);
        } finally {
            MDC.clear();
        }
    }

    private void handleFailure(Message sqsMessage, int attempt, Exception exception) {
        try {
            JobMessage job = objectMapper.readValue(sqsMessage.body(), JobMessage.class);
            MDC.put("jobId", job.jobId());
            String status = attempt >= 3 ? "FAILED" : "RETRYING";
            update(job.jobId(), status, attempt, exception.getMessage());
            log.warn("job failed jobId={} status={} attempt={} error={}",
                    job.jobId(), status, attempt, exception.getMessage());
        } catch (Exception nestedException) {
            log.error("could not record job failure attempt={}", attempt, nestedException);
        }
    }

    private void update(String jobId, String status, int attempt, String error) {
        Map<String, AttributeValue> values = error == null
                ? Map.of(
                        ":status", stringValue(status),
                        ":updatedAt", stringValue(Instant.now().toString()),
                        ":attempt", numberValue(attempt))
                : Map.of(
                        ":status", stringValue(status),
                        ":updatedAt", stringValue(Instant.now().toString()),
                        ":attempt", numberValue(attempt),
                        ":lastError", stringValue(error));

        String updateExpression = error == null
                ? "SET #status = :status, updatedAt = :updatedAt, attempt = :attempt REMOVE lastError"
                : "SET #status = :status, updatedAt = :updatedAt, attempt = :attempt, lastError = :lastError";

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(properties.tableName())
                .key(Map.of(
                        "pk", stringValue("JOB#" + jobId),
                        "sk", stringValue(JOB_SK)))
                .updateExpression(updateExpression)
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(values)
                .build());
    }

    private static int attempt(Message message) {
        String value = message.attributesAsStrings().get("ApproximateReceiveCount");
        if (value == null) {
            return 1;
        }
        return Integer.parseInt(value);
    }

    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private static AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue numberValue(long value) {
        return AttributeValue.builder().n(Long.toString(value)).build();
    }
}
