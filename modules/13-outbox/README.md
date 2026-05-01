# Module 13: Outbox Pattern

## Real-Life Project

Build reliable job-created event publishing. When a job is created, persist both the job and an outbox event in the same transaction. A publisher later sends the event to SQS/Kafka.

This avoids the classic partial failure: DB write succeeds but event publish fails.

## What To Build

- Create `jobs` and `outbox_events`.
- Write job + outbox event atomically.
- Add publisher that reads unpublished outbox events.
- Publish to SQS and mark event as sent.
- Add retry and dead-letter behavior for publish failures.
- Add metrics for outbox backlog and oldest event age.

## Production Concepts

- Direct DB write + message publish is not atomic.
- Outbox makes state change durable before async dispatch.
- Publisher must be idempotent.
- Consumers must still handle duplicate events.
- Backlog age is the key operational metric.

## Learning-Optimized Comments To Add In Code

```java
// The outbox row is committed with the business state so a process crash cannot
// lose the event after the job is created.
```

```java
// Marking an event as published happens after the broker accepts it. Consumers
// must still tolerate duplicates.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Broker down | fail SQS publish | outbox backlog grows |
| Publisher crash | stop after reading event | event remains retryable |
| Duplicate publish | publish succeeds but mark fails | consumer may receive duplicate |
| Old event | publisher stuck | oldest event age alarm |

## On-Call Runbook

1. Check outbox backlog count and oldest event age.
2. Check broker health and publisher logs.
3. Confirm business writes are still succeeding.
4. If backlog grows, scale/restart publisher or fix broker permission/config.
5. Avoid manual deletion unless event is proven invalid.
6. Replay safely after fixing root cause.

## Interview Talking Points

- Why outbox exists.
- Atomicity limits across DB and broker.
- Duplicate event handling.
- Backlog alarms and replay strategy.

