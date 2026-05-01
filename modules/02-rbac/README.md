# Module 02: Method-Level RBAC

## Real-Life Project

Build role and permission checks for user and admin APIs. Example: regular users can create jobs and view their own jobs; admins can view all jobs and trigger operational endpoints.

This is common in SaaS dashboards, internal tools, banking systems, and platform APIs.

## What To Build

- Add roles: `USER`, `SUPPORT`, `ADMIN`.
- Add permissions: `job:create`, `job:read:self`, `job:read:any`, `lab:trigger`.
- Protect endpoints with method-level checks.
- Return `403` when identity is valid but permission is missing.
- Add structured logs for denied access without logging secrets.
- Add tests for allowed and denied paths.

## Production Concepts

- `401` means unauthenticated.
- `403` means authenticated but not authorized.
- Roles are coarse; permissions are precise.
- Authorization logic should be centralized and testable.
- RBAC failures are security events and should be auditable.

## Learning-Optimized Comments To Add In Code

```java
// Authorization decisions stay near the service boundary so business methods do
// not silently rely on controller-only checks.
```

```java
// Return 403 for valid users without permission. Do not hide this as a 500.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| User calls admin API | token role `USER` | `403` |
| Missing permission mapping | valid admin token but permission not loaded | unexpected `403` |
| Over-permissive role | user can read all jobs | security bug |
| Role config drift | deployed role names differ from token roles | widespread `403` |

## On-Call Runbook

1. Check if spike is `401` or `403`.
2. For `403`, inspect role/permission claims and endpoint policy.
3. Check deploy timeline for authorization rule changes.
4. Check if only one tenant/role is affected.
5. Review audit logs for denied permission and subject.
6. Mitigate by rollback or targeted policy/config correction.

## Interview Talking Points

- Role vs permission design.
- Why deny-by-default matters.
- How to audit authorization decisions.
- How to safely migrate permissions without locking users out.

