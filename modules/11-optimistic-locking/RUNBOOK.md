# On-Call Runbook: Optimistic Locking Lab

## Symptom

Clients see `409 Conflict` while updating the same resource.

## Fast Checks

1. Confirm client sends expected version.
2. Check if another actor updated the resource first.
3. Re-read current state and retry only with fresh version.
4. Watch for clients blindly retrying stale versions.

