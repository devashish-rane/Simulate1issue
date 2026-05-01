# On-Call Runbook: Outbox Lab

## Symptom

Business writes succeed but downstream systems do not receive events.

## Fast Checks

1. Check outbox pending count and oldest event age.
2. Check publisher logs and broker errors.
3. Confirm job rows exist.
4. Replay only after fixing publisher/broker issue.
5. Consumers must tolerate duplicate events.

