# On-Call Runbook: Transaction Lab

## Symptom

Balances or state records look partially updated after an API failure.

## Fast Checks

1. Identify the business unit of work.
2. Check whether all writes happen inside one transaction.
3. Check logs for exception after first write.
4. Check if external calls are inside the transaction.
5. Verify rollback rules for checked/runtime exceptions.

## Mitigation

- Disable affected write path if partial writes continue.
- Roll back transaction-boundary changes.
- Repair data only after root cause is confirmed.

