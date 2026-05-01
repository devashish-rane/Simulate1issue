# On-Call Runbook: RBAC Lab

## Symptom

Users report `403 Forbidden` for APIs they previously accessed.

## Fast Checks

1. Confirm `401` vs `403`. `401` is identity, `403` is permission.
2. Identify endpoint and required permission.
3. Inspect token role and derived permissions.
4. Check whether one role, one tenant, or all users are affected.
5. Check deploy/config changes touching permission mapping.

## Logs Insights Query

```sql
fields @timestamp, @message
| filter @message like /authenticated|Forbidden|AccessDenied|permission/
| sort @timestamp desc
| limit 100
```

## Mitigation

- Roll back permission mapping if many valid users are blocked.
- Restore missing permission for affected role.
- Never grant broad admin permissions as a quick fix unless incident owner approves.

## Permanent Fix

- Add authorization matrix tests.
- Add audit logs for denied permission.
- Add metrics for `403` by endpoint and role.

