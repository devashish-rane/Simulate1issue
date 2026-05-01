# Module 08: JPA Transactions

## Real-Life Project

Build a small order/job creation flow with multiple database writes. The whole operation should commit together or roll back together.

This module is easiest with a relational DB later, but the same thinking applies to durable state in DynamoDB using conditional writes.

## What To Build

- Create a service method with multiple writes.
- Mark it with `@Transactional`.
- Demonstrate rollback on runtime exception.
- Demonstrate checked exception rollback rules.
- Add read-only transactions for query methods.
- Add tests that prove partial writes do not persist.

## Production Concepts

- Transaction boundaries belong in service layer, not controllers.
- Runtime exceptions roll back by default in Spring.
- Checked exceptions do not always roll back unless configured.
- External calls inside transactions increase lock time and failure coupling.
- Long transactions hurt concurrency.

## Learning-Optimized Comments To Add In Code

```java
// The transaction wraps the business unit of work, not the HTTP request.
```

```java
// Avoid slow network calls inside an open transaction because locks stay held
// while the dependency responds.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Mid-flow exception | throw after first write | no partial state |
| Checked exception | throw checked exception | observe rollback behavior |
| Slow dependency inside tx | sleep/call inside tx | increased lock time |
| Missing transaction | remove annotation | partial write risk |

## On-Call Runbook

1. Look for partial/inconsistent records.
2. Check logs for exceptions after first write.
3. Check DB lock/wait metrics if available.
4. Check deploys touching transaction boundaries.
5. Mitigate by disabling affected write path or rolling back.
6. Repair inconsistent data with audited script after root cause is known.

## Interview Talking Points

- Transaction boundary placement.
- Rollback rules in Spring.
- Why not to call external services inside transactions.
- Saga/outbox alternatives for distributed workflows.

