# Module 04: Tenant Data Isolation

## Real-Life Project

Build job storage so each tenant sees only its own jobs. Every read, write, update, and delete must be tenant-scoped.

This is one of the most important SaaS backend correctness requirements.

## What To Build

- Add `tenantId` to job records.
- Require tenant context for all job operations.
- Query by `tenantId + jobId`.
- Prevent direct lookup by only `jobId`.
- Add tests proving tenant A cannot read tenant B data.
- Add audit log fields for tenant-scoped access.

## Production Concepts

- Tenant isolation bugs are security incidents.
- Isolation must be enforced in repository/query layer, not only UI.
- Global IDs are not enough; every access path must include tenant.
- Admin/support access needs explicit elevated permission and audit trail.
- Data exports are especially risky.

## Learning-Optimized Comments To Add In Code

```java
// Every job lookup includes tenantId. A globally unique jobId alone is not an
// authorization boundary.
```

```java
// Support/admin bypasses must be explicit and audited.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Cross-tenant read | tenant A reads tenant B job ID | `404` or `403` |
| Missing tenant in query | repository method ignores tenant | data leak risk |
| Admin read | support role reads another tenant | allowed with audit |
| Export bug | export endpoint skips tenant filter | severe incident |

## On-Call Runbook

1. Treat suspected cross-tenant access as security severity.
2. Identify affected tenants and records.
3. Stop or restrict the endpoint if active leakage is possible.
4. Preserve logs and request IDs.
5. Check recent query/repository changes.
6. Patch query filters and add regression tests.
7. Coordinate security/customer notification process if real impact exists.

## Interview Talking Points

- `403` vs `404` for cross-tenant access.
- Why tenant filtering belongs in repository/service layer.
- How to audit support/admin data access.
- How to design table keys for tenant isolation.

