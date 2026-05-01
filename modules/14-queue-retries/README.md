# Module 14: Queue Consumer Retries

## Real-Life Project

Build robust worker processing with SQS retries, visibility timeout, max receive count, and DLQ. The worker should process normal jobs, retry transient jobs, and isolate poison jobs.

This already exists partly in `worker-service`; make it production-grade.

## What To Build

- Add explicit job attempt tracking.
- Tune visibility timeout above max processing time.
- Send poison messages to DLQ after max receives.
- Add idempotent worker updates.
- Add DLQ replay documentation.
- Add dashboard metrics: visible messages, oldest age, DLQ count.

## Production Concepts

- At-least-once delivery means duplicates are normal.
- Visibility timeout is not processing timeout.
- DLQ is a safety valve, not a fix.
- Poison messages should not block healthy work.
- Worker operations need idempotent state transitions.

## Learning-Optimized Comments To Add In Code

```java
// SQS can deliver the same message more than once, so job updates must be safe
// to repeat.
```

```java
// Failed messages are left undeleted so SQS can retry them after visibility
// timeout expires.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Slow job | processing exceeds expectation | visible backlog grows |
| Transient fail | fail twice then succeed | retries then success |
| Poison message | always fail | DLQ count increases |
| Visibility too short | worker takes longer than timeout | duplicate processing |

## On-Call Runbook

1. Check queue visible messages and oldest message age.
2. Check worker running task count and CPU/memory.
3. Check DLQ count and sample message body.
4. Classify transient vs poison vs capacity issue.
5. Scale workers for backlog only if downstream can handle it.
6. Replay DLQ only after code/config fix.

## Interview Talking Points

- At-least-once delivery.
- Visibility timeout and DLQ redrive.
- Idempotent consumers.
- Backpressure and worker scaling.

