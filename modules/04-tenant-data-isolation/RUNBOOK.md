# On-Call Runbook: Tenant Data Isolation Lab

## Symptom

A user reports seeing another tenant's data, or a tenant suddenly cannot see its own data.

## Fast Checks

1. Treat possible cross-tenant access as a security incident.
2. Preserve request IDs and logs.
3. Identify tenant ID, user ID, resource ID, and endpoint.
4. Verify repository/query filters include tenant ID.
5. Check support/admin bypass paths and audit logs.

## Mitigation

- Disable affected endpoint if leakage is possible.
- Roll back query or authorization change.
- Patch all lookup paths to include tenant ID.
- Add regression tests for cross-tenant reads.

