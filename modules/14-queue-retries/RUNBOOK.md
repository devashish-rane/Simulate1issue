# On-Call Runbook: Queue Retries Lab

## Fast Checks

1. Check visible messages and oldest age.
2. Check DLQ count.
3. Classify poison vs transient failures.
4. Scale workers only if downstream can handle increased load.
5. Replay DLQ only after fixing the cause.

