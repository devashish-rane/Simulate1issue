# Module 06: Request/Response DTOs

## Real-Life Project

Replace direct maps or entity exposure with DTOs for public APIs. Build separate request DTOs, response DTOs, and internal domain objects.

This keeps API contracts stable while internal implementation changes.

## What To Build

- Create DTOs for token, job creation, job status, lab responses, and errors.
- Map DTOs to domain commands/results.
- Avoid returning database or SDK models directly.
- Version DTOs when changing response shape.
- Add tests asserting API JSON shape.

## Production Concepts

- DTOs are the contract between backend and clients.
- Entity exposure leaks internal fields and creates migration risk.
- Response compatibility matters during rolling deploys.
- Null handling and default values need deliberate design.
- DTO mapping can be manual or library-based, but must be obvious.

## Learning-Optimized Comments To Add In Code

```java
// DTOs are intentionally separate from storage models so database changes do
// not automatically become API breaking changes.
```

```java
// Public responses should include only fields clients are allowed to depend on.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Extra field leak | expose internal entity | client sees sensitive/internal data |
| Breaking rename | rename JSON field | UI or client fails |
| Null surprise | omit optional field | frontend parsing issue |
| Version mismatch | old client hits new API | compatibility failure |

## On-Call Runbook

1. Check if failures started after API response/request shape changed.
2. Compare working and failing payloads.
3. Inspect frontend/client error reports.
4. Check API Gateway logs for `4xx` increase.
5. Roll back incompatible DTO changes or support both old/new fields.
6. Add contract tests before redeploying.

## Interview Talking Points

- DTO vs entity vs domain model.
- Backward-compatible API changes.
- API versioning strategies.
- Contract testing.

