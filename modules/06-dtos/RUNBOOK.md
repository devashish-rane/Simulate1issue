# On-Call Runbook: DTO Contract Lab

## Symptom

Frontend/client breaks after backend response shape changed.

## Fast Checks

1. Compare old and new response JSON.
2. Check for renamed/removed fields.
3. Check whether internal entity fields leaked.
4. Check deploy timeline for DTO changes.
5. Roll back or support both old and new fields.

## Permanent Fix

- Add contract tests.
- Version breaking API changes.
- Keep DTOs separate from database entities.

