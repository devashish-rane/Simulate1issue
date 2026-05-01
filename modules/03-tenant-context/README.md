# Module 03: Tenant Context Filter

## Real-Life Project

Build a tenant context filter that validates `X-Tenant-Id`, stores it for the request, and makes it available to services, logs, metrics, and authorization checks.

This models SaaS systems where every request belongs to a customer account, workspace, organization, or merchant.

## What To Build

- Require `X-Tenant-Id` on tenant-scoped APIs.
- Validate tenant format and membership for the authenticated user.
- Store tenant in request-scoped context.
- Add tenant ID to logs and metrics.
- Reject missing tenant with structured `400` or `401/403` depending on design.
- Keep non-tenant endpoints public or system-scoped.

## Production Concepts

- Tenant context is a security boundary, not just a filter field.
- Tenant must be validated against the user identity.
- Tenant ID should be propagated to async jobs.
- Logs and metrics should include tenant safely, but avoid high-cardinality explosions in metrics.
- Background workers need explicit tenant context because no HTTP request exists.

## Learning-Optimized Comments To Add In Code

```java
// Tenant context is derived once at the edge of the request and reused by lower
// layers so every query and log line agrees on the active tenant.
```

```java
// Do not trust tenant IDs from clients until membership is verified.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Missing tenant | no `X-Tenant-Id` | structured rejection |
| Invalid tenant | unknown tenant | `403` or `404` depending policy |
| Cross-tenant request | user from tenant A asks tenant B | denied |
| Lost tenant in queue | job message omits tenant | worker cannot process safely |

## On-Call Runbook

1. Check if failures are tenant-specific.
2. Compare affected tenant IDs in logs.
3. Confirm user-to-tenant membership data.
4. Check recent auth or tenant config deployments.
5. Verify async messages contain tenant ID.
6. Mitigate by correcting tenant config, replaying failed jobs, or rolling back filter/policy changes.

## Interview Talking Points

- How to prevent cross-tenant data leakage.
- Why tenant context must be propagated to workers.
- Difference between tenant ID in header, token claim, and database row.
- High-cardinality tenant labels in metrics.

