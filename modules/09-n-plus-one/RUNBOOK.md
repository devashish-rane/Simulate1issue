# On-Call Runbook: N+1 Query Lab

## Symptom

List endpoint latency increases as data grows, but status remains `200`.

## Fast Checks

1. Check p95/p99 latency for list endpoint.
2. Check DB query count and slow query logs.
3. Compare page size and response expansion flags.
4. Inspect recent DTO/entity/serializer changes.
5. Use entity graph/projection/fetch join to avoid query explosion.

