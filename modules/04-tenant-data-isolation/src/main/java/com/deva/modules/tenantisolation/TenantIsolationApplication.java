package com.deva.modules.tenantisolation;

import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class TenantIsolationApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantIsolationApplication.class, args);
    }
}

record JobRecord(String tenantId, String jobId, String owner, String status) {
}

@RestController
class JobIsolationController {
    private final List<JobRecord> jobs = List.of(
            new JobRecord("tenant-alpha", "job-100", "user-1", "PENDING"),
            new JobRecord("tenant-beta", "job-200", "user-2", "SUCCEEDED"));

    @GetMapping("/api/jobs/{jobId}")
    Map<String, Object> getJob(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String jobId) {
        JobRecord job = jobs.stream()
                .filter(candidate -> candidate.tenantId().equals(tenantId))
                .filter(candidate -> candidate.jobId().equals(jobId))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);

        return Map.of(
                "tenantId", job.tenantId(),
                "jobId", job.jobId(),
                "owner", job.owner(),
                "status", job.status());
    }

    @GetMapping("/api/support/jobs/{jobId}")
    Map<String, Object> supportLookup(
            @RequestHeader("X-Support-Role") String role,
            @PathVariable String jobId) {
        if (!"SUPPORT".equals(role)) {
            throw new JobNotFoundException();
        }
        JobRecord job = jobs.stream()
                .filter(candidate -> candidate.jobId().equals(jobId))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);
        return Map.of("tenantId", job.tenantId(), "jobId", job.jobId(), "status", job.status(), "access", "audited-support");
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class JobNotFoundException extends RuntimeException {
}

