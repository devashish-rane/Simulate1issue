# Module 07: Bean Validation

## Real-Life Project

Add validation to all request DTOs. Invalid input should fail before business logic, return a clean `400`, and tell clients exactly what to fix.

Every production API needs predictable validation.

## What To Build

- Use `@Valid` request bodies.
- Add constraints such as `@NotBlank`, `@Size`, `@Min`, `@Max`, and `@Pattern`.
- Add custom validator for business-like formats such as `tenantId` or `clientId`.
- Convert validation exceptions into structured error responses.
- Add tests for invalid payloads.

## Production Concepts

- Validation prevents bad state from entering the system.
- Syntactic validation belongs at API boundary.
- Business validation belongs in service/domain layer.
- Error responses should be field-specific.
- Never trust frontend validation alone.

## Learning-Optimized Comments To Add In Code

```java
// Bean validation handles request shape and simple field constraints before
// the service layer sees the command.
```

```java
// Business rules remain in the service layer because they may require current
// state, permissions, or dependencies.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Missing required field | omit `clientId` | `400` with field error |
| Too large input | oversized name/body | `400` |
| Invalid enum | unknown job type | `400` |
| Business invalid | unsupported plan | controlled domain error |

## On-Call Runbook

1. Check `4xx` spike by path.
2. Inspect validation error fields.
3. Determine if clients changed payload format.
4. Check deploy timeline for stricter validation.
5. Mitigate by rollback, client fix, or compatibility fallback.
6. Add dashboard split for validation failures by endpoint.

## Interview Talking Points

- Bean validation vs business validation.
- Why `400` is not a server incident unless rate spikes unexpectedly.
- How to make validation messages safe and useful.
- Backward compatibility when tightening validation.

