# Module 18: Structured Logging

## Real-Life Project

Convert Spring logs to structured JSON and include correlation fields: service, trace ID, span ID, request ID, tenant ID, user ID, path, status, error code, and latency.

This is the missing maturity step you noticed while investigating traces.

## What To Build

- Emit JSON logs from all services.
- Add OpenTelemetry trace/span IDs to MDC.
- Add request ID and API Gateway IDs when available.
- Add tenant/user context after auth.
- Log slow successful requests above threshold.
- Keep expected business failures as structured `WARN`.

## Production Concepts

- Plain text logs are hard to query reliably.
- Trace ID links logs to traces.
- High-cardinality fields are okay in logs, risky in metrics.
- Logs must avoid secrets and PII.
- Slow 200s need logs or metrics because exception handlers do not run.

## Learning-Optimized Comments To Add In Code

```java
// Logs include trace_id and span_id so an X-Ray trace can be joined back to
// exact application logs.
```

```java
// User and tenant identifiers help impact analysis, but secrets and raw tokens
// must never be logged.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Trace lookup | copy trace ID from UI | logs found by trace ID |
| Slow 200 | profile delay | slow request log appears |
| Business 503 | inventory failure | structured warning |
| Unhandled 500 | feature file missing | error log with stack trace |

## On-Call Runbook

1. Start from UI/API request ID or X-Ray trace ID.
2. Search service logs by `trace_id`.
3. Filter by `level=ERROR` or `error_code`.
4. Use tenant/user fields for impact.
5. Use path/status/latency fields for timeline.
6. If trace ID is missing, file instrumentation gap and fall back to timestamp search.

## Interview Talking Points

- Structured vs text logs.
- Log-trace correlation.
- MDC and OpenTelemetry context.
- What not to log.

