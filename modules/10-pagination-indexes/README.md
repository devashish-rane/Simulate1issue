# Module 10: Pagination With Indexes

## Real-Life Project

Build a scalable job list API with filters, sorting, and pagination. Support both offset pagination and keyset pagination.

List APIs are everywhere, and poor pagination is a common production latency issue.

## What To Build

- `GET /api/jobs?status=&createdAfter=&limit=&cursor=`.
- Add stable sorting by `createdAt` and `jobId`.
- Implement offset pagination first.
- Implement cursor/keyset pagination for large datasets.
- Add database indexes matching filter and sort patterns.
- Return `nextCursor` in responses.

## Production Concepts

- Offset pagination becomes expensive on deep pages.
- Sorting must be deterministic.
- Indexes must match query shape.
- Large page sizes cause memory and latency issues.
- Cursor tokens should be opaque and tamper-resistant.

## Learning-Optimized Comments To Add In Code

```java
// Keyset pagination uses the last seen sort values instead of asking the
// database to skip an increasingly large number of rows.
```

```java
// Sort order includes jobId as a tiebreaker so pagination is stable when many
// rows share the same timestamp.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Deep offset page | request page 10000 | high DB latency |
| Large page size | `limit=5000` | high memory/latency |
| Unstable sort | duplicate timestamps | missing/duplicate rows |
| Missing index | filter by unindexed field | slow queries |

## On-Call Runbook

1. Identify slow endpoint and query parameters.
2. Check page size, page number, sort, and filters.
3. Check DB slow query logs and index usage.
4. Mitigate by lowering max page size or blocking pathological query.
5. Add/adjust index and migrate safely.
6. Prefer keyset pagination for high-volume flows.

## Interview Talking Points

- Offset vs keyset pagination.
- Stable sort requirements.
- Index design from access patterns.
- API compatibility when changing pagination.

