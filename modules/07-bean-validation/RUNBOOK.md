# On-Call Runbook: Bean Validation Lab

## Symptom

Clients see a sudden spike in `400 VALIDATION_FAILED`.

## Fast Checks

1. Check which field fails most often.
2. Compare payload from working vs failing client version.
3. Check if backend validation was tightened.
4. Decide whether failures are client bug or backward-incompatible backend change.

## Mitigation

- Roll back stricter validation if it broke valid clients.
- Support both old and new formats during migration.
- Add field-level dashboard for repeated validation issues.

