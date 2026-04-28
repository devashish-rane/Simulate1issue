package com.deva.learn.job;

import org.springframework.stereotype.Component;

import com.deva.learn.config.AppProperties;
import com.deva.learn.exception.JobSubmissionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
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
                    .build());
        } catch (JsonProcessingException exception) {
            throw new JobSubmissionException(exception);
        } catch (RuntimeException exception) {
            throw new JobSubmissionException(exception);
        }
    }
}
