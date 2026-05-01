package com.deva.modules.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@SpringBootApplication
public class OutboxApplication {
    public static void main(String[] args) {
        SpringApplication.run(OutboxApplication.class, args);
    }
}

@Entity
class JobEntity {
    @Id
    private String jobId;
    private String status;

    protected JobEntity() {
    }

    JobEntity(String jobId, String status) {
        this.jobId = jobId;
        this.status = status;
    }

    String jobId() {
        return jobId;
    }
}

@Entity
class OutboxEvent {
    @Id
    @GeneratedValue
    private Long id;
    private String aggregateId;
    private String eventType;
    private String payload;
    private boolean published;
    private Instant createdAt;

    protected OutboxEvent() {
    }

    OutboxEvent(String aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    Long id() {
        return id;
    }

    String aggregateId() {
        return aggregateId;
    }

    boolean published() {
        return published;
    }

    void markPublished() {
        published = true;
    }
}

interface JobRepository extends JpaRepository<JobEntity, String> {
}

interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}

@Service
class JobCreationService {
    private final JobRepository jobRepository;
    private final OutboxRepository outboxRepository;

    JobCreationService(JobRepository jobRepository, OutboxRepository outboxRepository) {
        this.jobRepository = jobRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    String createJob(String type) {
        String jobId = "job-" + Instant.now().toEpochMilli();
        jobRepository.save(new JobEntity(jobId, "CREATED"));
        outboxRepository.save(new OutboxEvent(jobId, "JobCreated", "{\"type\":\"" + type + "\"}"));
        return jobId;
    }
}

@RestController
class OutboxController {
    private final JobCreationService jobCreationService;
    private final OutboxRepository outboxRepository;

    OutboxController(JobCreationService jobCreationService, OutboxRepository outboxRepository) {
        this.jobCreationService = jobCreationService;
        this.outboxRepository = outboxRepository;
    }

    @PostMapping("/api/jobs")
    Map<String, Object> create(@RequestParam(defaultValue = "scan") String type) {
        return Map.of("jobId", jobCreationService.createJob(type), "outbox", "event-persisted");
    }

    @GetMapping("/api/outbox")
    List<Map<String, Object>> pending() {
        return outboxRepository.findByPublishedFalseOrderByCreatedAtAsc().stream()
                .map(event -> Map.<String, Object>of("id", event.id(), "aggregateId", event.aggregateId(), "published", event.published()))
                .toList();
    }

    @PostMapping("/api/outbox/publish-one")
    Map<String, Object> publishOne() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return Map.of("published", false);
        }
        OutboxEvent event = pending.get(0);
        event.markPublished();
        outboxRepository.save(event);
        return Map.of("published", true, "eventId", event.id());
    }
}

