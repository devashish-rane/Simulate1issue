# On-Call Runbook: Health Design Lab

## Fast Checks

1. Separate liveness from readiness.
2. Do not restart healthy app processes because a dependency is down.
3. Use business canaries for real user flow checks.
4. Keep ALB/readiness checks cheap and stable.

