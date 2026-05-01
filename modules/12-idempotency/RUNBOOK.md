# On-Call Runbook: Idempotency Lab

## Symptom

Clients report duplicate jobs/orders after retrying timed-out requests.

## Fast Checks

1. Check whether client sent `Idempotency-Key`.
2. Search by key and request hash.
3. Verify same key is not reused for different payloads.
4. Check retry policy changes.
5. Confirm store TTL did not expire too early.

