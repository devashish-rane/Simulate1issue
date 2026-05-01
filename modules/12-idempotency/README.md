# Module 12: Idempotency Keys

## Real-Life Project

Build idempotent job creation. If a client times out and retries with the same idempotency key, the service must not create duplicate jobs.

This is essential for payments, orders, booking, provisioning, and async job creation.

## What To Build

- Require `Idempotency-Key` for `POST /api/jobs`.
- Store key, request hash, response, status, and expiry.
- Return the same response for duplicate identical requests.
- Reject same key with different request body.
- Handle in-progress requests safely.
- Add TTL cleanup.

## Production Concepts

- Idempotency protects against client retries and network uncertainty.
- Key scope matters: usually tenant + user + endpoint + key.
- Store request hash to detect accidental key reuse.
- In-progress behavior must be explicit.
- Idempotency is not the same as deduplication after side effects.

## Learning-Optimized Comments To Add In Code

```java
// The idempotency record is written before side effects so a retry can observe
// that this operation is already in progress.
```

```java
// A reused key with a different payload is rejected because it may represent a
// client bug that would otherwise hide data corruption.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Retry same request | same key/body twice | same job ID returned |
| Reuse key different body | same key/new payload | `409` or `422` |
| Timeout then retry | client timeout after create | no duplicate job |
| In-progress retry | retry during first execution | `202` existing operation |

## On-Call Runbook

1. Check duplicate create reports.
2. Search by idempotency key, tenant, and endpoint.
3. Confirm request hashes match.
4. Check if clients changed retry policy.
5. Check storage TTL or conditional write failures.
6. Mitigate by replay blocking, data deduplication, or temporary client retry reduction.

## Interview Talking Points

- Idempotency key scoping.
- Handling same key with different payload.
- Race conditions during first write.
- Difference between idempotency and exactly-once processing.

