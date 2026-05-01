package com.deva.modules.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class IdempotencyApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdempotencyApplication.class, args);
    }
}

record CreateJobRequest(String type, String payload) {
}

record CreateJobResponse(String jobId, String status, boolean replayed) {
}

record IdempotencyRecord(String requestHash, CreateJobResponse response) {
}

@RestController
class IdempotentJobController {
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    @PostMapping("/api/jobs")
    CreateJobResponse create(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CreateJobRequest request) {
        String requestHash = hash(request.type() + "|" + request.payload());
        IdempotencyRecord existing = store.get(key);
        if (existing != null) {
            if (!existing.requestHash().equals(requestHash)) {
                throw new IdempotencyKeyConflictException();
            }
            return new CreateJobResponse(existing.response().jobId(), existing.response().status(), true);
        }

        CreateJobResponse response = new CreateJobResponse("job-" + Math.abs(key.hashCode()), "ACCEPTED", false);
        store.put(key, new IdempotencyRecord(requestHash, response));
        return response;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
class IdempotencyKeyConflictException extends RuntimeException {
}

