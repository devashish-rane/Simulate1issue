# On-Call Runbook: Tenant Context Lab

## Symptom

Tenant-scoped APIs reject requests or show tenant-specific failures.

## Fast Checks

1. Confirm `X-Tenant-Id`, user identity, and memberships.
2. Determine if one tenant or all tenants are affected.
3. Search logs by tenant ID.
4. Check whether async jobs include tenant context.
5. Check recent auth/tenant membership/config changes.

## Logs Insights Query

```sql
fields @timestamp, @message
| filter @message like /TENANT_|tenantId/
| sort @timestamp desc
| limit 100
```

## Mitigation

- Restore missing tenant membership/config.
- Roll back tenant filter changes if widespread.
- Pause worker replay if tenant context is missing from messages.

