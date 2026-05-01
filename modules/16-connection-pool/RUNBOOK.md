# On-Call Runbook: Connection Pool Lab

## Fast Checks

1. Check active, idle, pending connections.
2. Check DB max connections.
3. Check slow queries and long transactions.
4. Do not blindly increase app pool size beyond DB capacity.
5. Roll back query changes that hold connections too long.

