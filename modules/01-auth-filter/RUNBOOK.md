# On-Call Runbook: Auth Filter Lab

## Page Summary

Protected APIs are returning auth-related failures.

## First Question

Classify the status code:

| Status | Meaning | First Owner |
| --- | --- | --- |
| `401` | Missing, malformed, expired, or inactive token | client/auth config |
| `403` | Authenticated but not allowed | RBAC/policy |
| `503` | Auth provider could not validate token | backend dependency |
| `500` | Unexpected bug in auth filter or security config | service owner |

## Fast Checks

1. Confirm path and status.
2. Check whether failures are all protected APIs or one endpoint.
3. Check whether public `/public/ping` and `/actuator/health` still work.
4. Search service logs by `requestId`, `AUTH_`, or `authentication`.
5. Verify clients are sending `Authorization: Bearer <token>`.
6. Check auth provider latency and availability if failures are `503`.

## Logs Insights Queries

Expected auth denials:

```sql
fields @timestamp, @message
| filter @message like /authentication rejected|AUTH_/
| sort @timestamp desc
| limit 100
```

Unexpected auth filter failures:

```sql
fields @timestamp, @message
| filter @message like /authentication failed unexpectedly|Exception|ERROR/
| sort @timestamp desc
| limit 100
```

## Impact Questions

- How many requests are failing?
- Is it one client, one tenant, or all clients?
- Did this start after deploy or token config rotation?
- Are failures `401` only, or are `503` dependency failures present?

## Mitigation

- For widespread `401`, rollback token format/client auth change.
- For `503`, restore auth provider or reduce dependency timeout/retry pressure.
- For unexpected `500`, rollback the auth filter change.
- Do not bypass authentication in production without explicit incident commander approval.

## Permanent Fixes

- Add trace ID and request ID to every auth log.
- Add metrics for `AUTH_MISSING`, `AUTH_INACTIVE`, and auth provider latency.
- Add contract tests for token format.
- Add dashboard split by status and error code.

