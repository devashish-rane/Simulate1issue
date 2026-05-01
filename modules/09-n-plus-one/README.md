# Module 09: N+1 Query Debugging

## Real-Life Project

Build a list endpoint that returns parent records and child details. First implement it with an N+1 query problem, then fix it.

This is one of the most common performance bugs in ORM-backed services.

## What To Build

- Add entities such as `Customer -> Jobs` or `Order -> Items`.
- Build list API that accidentally loads children lazily in a loop.
- Enable SQL/query count logs in local profile.
- Fix using fetch join, entity graph, batch size, or explicit query projection.
- Add performance test or query-count assertion.

## Production Concepts

- N+1 often shows as latency increase without errors.
- It gets worse as data grows.
- Pagination can hide or amplify it.
- Serializers can trigger lazy loads unexpectedly.
- Query count is an important diagnostic, not just query duration.

## Learning-Optimized Comments To Add In Code

```java
// Avoid touching lazy collections while serializing list responses; it can turn
// one request into hundreds of queries.
```

```java
// This projection fetches exactly what the API needs and avoids accidental ORM
// graph traversal.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| More rows | increase list size | latency climbs sharply |
| Child serialization | include child count/details | query count spikes |
| Offset pagination | deep page | DB time increases |
| Missing index | filter without index | slow query |

## On-Call Runbook

1. Confirm endpoint latency spike with no 5xx.
2. Check DB CPU, query count, and slow query logs.
3. Compare affected request payload/page size.
4. Check recent code changes to response shape or ORM mappings.
5. Mitigate by reducing page size, disabling expanded fields, or rollback.
6. Patch query with fetch strategy/projection and add query-count test.

## Interview Talking Points

- Lazy vs eager loading.
- Fetch join vs projection.
- Why serializers can cause database queries.
- How to detect N+1 in production.

