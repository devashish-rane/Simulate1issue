package com.deva.worker.job;

public record JobMessage(
        String jobId,
        String subject,
        String jobType,
        boolean slow,
        boolean fail,
        boolean poison,
        int durationMs) {
}
