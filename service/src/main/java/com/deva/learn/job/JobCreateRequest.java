package com.deva.learn.job;

public record JobCreateRequest(
        String jobType,
        boolean slow,
        boolean fail,
        boolean poison,
        Integer durationMs) {
    int safeDurationMs() {
        if (durationMs == null) {
            return slow ? 5_000 : 500;
        }
        return Math.max(100, Math.min(durationMs, 15_000));
    }
}
