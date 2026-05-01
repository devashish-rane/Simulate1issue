# On-Call Runbook: Pagination Lab

## Symptom

List endpoint becomes slow for large pages or deep offsets.

## Fast Checks

1. Inspect `page`, `limit`, sort, and filters.
2. Check whether offset is deep.
3. Confirm max page size is enforced.
4. Prefer cursor pagination for high-volume lists.
5. Match DB index to filter and sort access pattern.

