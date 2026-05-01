package com.deva.learn.job;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.deva.learn.observability.TraceContext;
import com.deva.learn.config.AppProperties;
import com.deva.learn.exception.JobSubmissionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
class JobQueuePublisher {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    JobQueuePublisher(SqsClient sqsClient, ObjectMapper objectMapper, AppProperties properties) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = properties.jobs().queueUrl();
    }

    void publish(JobMessage message) {
        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(message))
                    .messageAttributes(messageAttributes())
                    .build());
        } catch (JsonProcessingException exception) {
            throw new JobSubmissionException(exception);
        } catch (RuntimeException exception) {
            throw new JobSubmissionException(exception);
        }
    }

    private static Map<String, MessageAttributeValue> messageAttributes() {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        putAttribute(attributes, "requestId", MDC.get("requestId"));
        putAttribute(attributes, "producerTraceId", TraceContext.currentTraceId());
        putAttribute(attributes, "producerSpanId", TraceContext.currentSpanId());
        return attributes;
    }

    private static void putAttribute(Map<String, MessageAttributeValue> attributes, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        attributes.put(name, MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build());
    }
}
