# Module 16: Connection Pool Tuning

## Real-Life Project

Build a database-backed endpoint and tune HikariCP connection pool settings. Simulate DB saturation and learn how it appears in app metrics.

This is common in Spring services using relational databases.

## What To Build

- Add HikariCP configuration.
- Expose pool metrics through Micrometer.
- Create endpoint that uses DB connection for normal queries.
- Simulate slow queries or long transactions.
- Tune `maximumPoolSize`, `connectionTimeout`, and leak detection.
- Add alarm for connection acquisition timeout.

## Production Concepts

- More connections are not always better.
- Pool exhaustion causes app latency even if CPU is low.
- Long transactions hold connections.
- DB max connections is shared across services.
- Connection leak detection helps find missing close/transaction issues.

## Learning-Optimized Comments To Add In Code

```java
// Pool size is tied to DB capacity and request concurrency, not simply CPU count.
```

```java
// Slow transactions hold connections longer and can make unrelated requests wait.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Pool exhaustion | many concurrent slow queries | connection timeout |
| Long transaction | sleep inside transaction | active connections stay high |
| DB saturation | lower DB capacity | query latency and pool wait |
| Leak | connection not returned | pool drains over time |

## On-Call Runbook

1. Check DB connection count and app pool active/idle/pending metrics.
2. Check slow query logs and transaction duration.
3. Check if traffic spike or deploy changed query behavior.
4. Mitigate by reducing traffic, rolling back, killing bad queries, or scaling DB if valid.
5. Avoid blindly increasing pool size beyond DB capacity.
6. Add query/index fix if saturation is query-driven.

## Interview Talking Points

- Pool size vs DB capacity.
- Symptoms of pool exhaustion.
- Why long transactions hurt unrelated requests.
- HikariCP metrics to monitor.

