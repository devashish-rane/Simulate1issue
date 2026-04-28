package com.deva.learn.job;

public record JobResponse(
        String jobId,
        String status,
        String subject,
        String jobType,
        boolean slow,
        boolean fail,
        boolean poison,
        int durationMs,
        String createdAt,
        String updatedAt,
        Integer attempt,
        String lastError) {
}
