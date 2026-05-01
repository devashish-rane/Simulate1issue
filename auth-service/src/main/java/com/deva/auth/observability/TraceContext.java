package com.deva.auth.observability;

import org.slf4j.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public final class TraceContext {
    private TraceContext() {
    }

    public static void putCurrentSpan() {
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            MDC.put("traceId", context.getTraceId());
            MDC.put("spanId", context.getSpanId());
        }
    }

    public static void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
