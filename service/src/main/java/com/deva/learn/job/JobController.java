package com.deva.learn.job;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.MDC;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final TokenValidator tokenValidator;
    private final JobStore jobStore;
    private final JobQueuePublisher jobQueuePublisher;

    JobController(TokenValidator tokenValidator, JobStore jobStore, JobQueuePublisher jobQueuePublisher) {
        this.tokenValidator = tokenValidator;
        this.jobStore = jobStore;
        this.jobQueuePublisher = jobQueuePublisher;
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) JobCreateRequest request) {
        String subject = tokenValidator.validate(authorization);
        JobCreateRequest safeRequest = request == null
                ? new JobCreateRequest("demo", false, false, false, null)
                : request;

        String jobId = UUID.randomUUID().toString();
        MDC.put("jobId", jobId);
        String now = Instant.now().toString();
        JobMessage message = new JobMessage(
                jobId,
                subject,
                safeJobType(safeRequest.jobType()),
                safeRequest.slow(),
                safeRequest.fail(),
                safeRequest.poison(),
                safeRequest.safeDurationMs());

        jobStore.create(message, now);
        jobQueuePublisher.publish(message);

        return ResponseEntity.accepted()
                .location(URI.create("/api/jobs/" + jobId))
                .body(Map.of(
                        "jobId", jobId,
                        "status", "PENDING",
                        "statusUrl", "/api/jobs/" + jobId));
    }

    @GetMapping("/{jobId}")
    ResponseEntity<JobResponse> get(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String jobId) {
        tokenValidator.validate(authorization);
        return ResponseEntity.ok(jobStore.get(jobId));
    }

    private static String safeJobType(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            return "demo";
        }
        return jobType.trim();
    }
}
