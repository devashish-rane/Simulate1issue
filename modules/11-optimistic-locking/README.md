# Module 11: Optimistic Locking

## Real-Life Project

Build concurrent job update behavior. Two callers read the same job and both attempt to update it. One should win; the other should receive a conflict.

This is common in inventory, booking, workflow state machines, and admin tools.

## What To Build

- Add a `version` field to mutable records.
- Require expected version on update.
- Return `409 CONFLICT` on stale version.
- Add retry strategy for safe automatic retries.
- Add tests with concurrent updates.

## Production Concepts

- Optimistic locking prevents lost updates.
- Conflict is not always a server error.
- Clients need to re-read and retry or show conflict UI.
- Conditional writes in DynamoDB serve the same purpose.
- State machines should validate allowed transitions too.

## Learning-Optimized Comments To Add In Code

```java
// The version check prevents a stale client from overwriting a newer decision.
```

```java
// Conflict responses tell clients to re-read state instead of blindly retrying
// the same stale update.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Concurrent update | two requests use same version | one `200`, one `409` |
| Missing version | update without expected version | reject |
| Blind retry | client retries stale update | repeated `409` |
| State transition race | worker and user update same job | conflict or invalid transition |

## On-Call Runbook

1. Check if `409` conflicts increased.
2. Identify affected endpoint and client behavior.
3. Confirm if clients are retrying stale versions.
4. Check for new concurrency source, worker change, or UI change.
5. Mitigate by disabling conflicting automation or fixing retry behavior.
6. Add metrics for conflict rate by endpoint.

## Interview Talking Points

- Optimistic vs pessimistic locking.
- Why `409` is usually correct for stale writes.
- Conditional writes in DynamoDB.
- User experience for conflict resolution.

