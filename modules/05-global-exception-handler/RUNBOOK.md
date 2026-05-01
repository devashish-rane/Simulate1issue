# On-Call Runbook: Global Exception Handler Lab

## Fast Checks

1. Find status, path, and error code.
2. Controlled app errors have stable codes such as `UNSUPPORTED_PLAN`.
3. Unexpected bugs become `INTERNAL_ERROR` and need stack trace review.
4. Check whether error rate changed after deploy.

## Logs Insights Query

```sql
fields @timestamp, @message
| filter @message like /request failed|INTERNAL_ERROR|Exception/
| sort @timestamp desc
| limit 100
```

## Mitigation

- Controlled business errors: evaluate rate and client behavior.
- Dependency errors: restore dependency or fail fast.
- Unexpected errors: rollback or hotfix.

