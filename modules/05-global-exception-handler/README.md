# Module 05: Global Exception Handler

## Real-Life Project

Build a production error model for all services. Every expected business failure should return a stable error code, safe message, HTTP status, and useful debugging context.

This is already partly present in `consumer-service`; extend it into a complete standard.

## What To Build

- Define `ApiException` with `errorCode`, `status`, `safeMessage`, and details.
- Handle validation errors, auth errors, business errors, dependency errors, and unhandled errors separately.
- Log expected failures at `WARN`; unexpected failures at `ERROR`.
- Return safe messages to clients.
- Include `requestId`, `path`, and timestamp in error responses.
- Add tests for error body shape.

## Production Concepts

- Not all exceptions are bugs.
- Expected business exceptions should be controlled and structured.
- Unexpected exceptions should be logged with stack trace and mapped to generic `500`.
- Error codes are more stable than messages.
- Client-facing messages should not leak internals.

## Learning-Optimized Comments To Add In Code

```java
// Expected domain failures are logged as WARN because they are operational
// signals, not necessarily code defects.
```

```java
// Unexpected exceptions return a generic message to clients but keep the stack
// trace in service logs for diagnosis.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Business error | invalid plan | structured `4xx` |
| Dependency unavailable | simulated inventory outage | structured `503` |
| Unhandled exception | missing config file | generic `500`, stack trace in logs |
| Validation failure | invalid request body | field-level error response |

## On-Call Runbook

1. Classify error: expected business, client validation, dependency, or unhandled.
2. Use API Gateway logs for status/path/count.
3. Use service logs for `errorCode` and exception class.
4. For controlled errors, inspect rate and business impact.
5. For unhandled errors, find stack trace and recent deployments.
6. Mitigate by rollback, config fix, dependency recovery, or feature disable.

## Interview Talking Points

- Why exception handling is part of API design.
- Difference between client-safe message and internal log detail.
- `WARN` vs `ERROR` decisions.
- Stable error codes for frontend and support teams.

