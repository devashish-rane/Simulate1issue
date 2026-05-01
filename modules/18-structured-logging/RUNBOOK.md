# On-Call Runbook: Structured Logging Lab

## Fast Checks

1. Search by trace ID first.
2. Filter by tenant/user only in logs, not high-cardinality metrics.
3. Confirm no raw tokens or secrets are logged.
4. If trace ID is missing, check correlation filter/instrumentation.

