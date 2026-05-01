package com.deva.worker.observability;

import org.slf4j.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

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

    public static void putMessageAttributes(Message message) {
        putAttribute(message, "requestId", "requestId");
        putAttribute(message, "producerTraceId", "producerTraceId");
        putAttribute(message, "producerSpanId", "producerSpanId");
    }

    private static void putAttribute(Message message, String attributeName, String mdcKey) {
        MessageAttributeValue value = message.messageAttributes().get(attributeName);
        if (value != null && value.stringValue() != null && !value.stringValue().isBlank()) {
            MDC.put(mdcKey, value.stringValue());
        }
    }
}
