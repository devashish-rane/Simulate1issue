package com.deva.learn.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class JobNotFoundException extends ApiException {
    public JobNotFoundException(String jobId) {
        super(HttpStatus.NOT_FOUND,
                "JOB_NOT_FOUND",
                "Job not found",
                Map.of("jobId", jobId));
    }
}
