package com.deva.modules.locking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class LockingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LockingApplication.class, args);
    }
}

record VersionedJob(String jobId, String status, long version) {
}

@RestController
class VersionedJobController {
    private final Map<String, VersionedJob> jobs = new ConcurrentHashMap<>(Map.of(
            "job-1", new VersionedJob("job-1", "PENDING", 1)));

    @GetMapping("/api/jobs/{jobId}")
    VersionedJob get(@PathVariable String jobId) {
        VersionedJob job = jobs.get(jobId);
        if (job == null) {
            throw new JobNotFoundException();
        }
        return job;
    }

    @PatchMapping("/api/jobs/{jobId}")
    VersionedJob update(
            @PathVariable String jobId,
            @RequestParam String status,
            @RequestParam long expectedVersion) {
        return jobs.compute(jobId, (id, current) -> {
            if (current == null) {
                throw new JobNotFoundException();
            }
            if (current.version() != expectedVersion) {
                throw new VersionConflictException();
            }
            return new VersionedJob(id, status, current.version() + 1);
        });
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class JobNotFoundException extends RuntimeException {
}

@ResponseStatus(HttpStatus.CONFLICT)
class VersionConflictException extends RuntimeException {
}

