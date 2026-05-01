package com.deva.modules.pagination;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PaginationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaginationApplication.class, args);
    }
}

record Job(String jobId, String status, Instant createdAt) {
}

@RestController
class JobListController {
    private final List<Job> jobs = IntStream.rangeClosed(1, 200)
            .mapToObj(i -> new Job("job-%03d".formatted(i), i % 2 == 0 ? "SUCCEEDED" : "PENDING",
                    Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i)))
            .sorted(Comparator.comparing(Job::createdAt).thenComparing(Job::jobId))
            .toList();

    @GetMapping("/api/jobs/offset")
    Map<String, Object> offset(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        int from = Math.min(Math.max(page, 0) * safeLimit, jobs.size());
        int to = Math.min(from + safeLimit, jobs.size());
        return Map.of(
                "items", jobs.subList(from, to),
                "page", page,
                "limit", safeLimit,
                "warning", page > 20 ? "deep-offset-can-be-slow" : "ok");
    }

    @GetMapping("/api/jobs/cursor")
    Map<String, Object> cursor(@RequestParam(required = false) String afterJobId, @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        int start = 0;
        if (afterJobId != null) {
            for (int i = 0; i < jobs.size(); i++) {
                if (jobs.get(i).jobId().equals(afterJobId)) {
                    start = i + 1;
                    break;
                }
            }
        }
        List<Job> page = jobs.subList(start, Math.min(start + safeLimit, jobs.size()));
        String nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).jobId();
        return Map.of("items", page, "nextCursor", nextCursor, "limit", safeLimit);
    }
}

