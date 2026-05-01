# On-Call Runbook: Config And Secrets Lab

## Fast Checks

1. Check if failure started after config or secret rotation.
2. Confirm required properties loaded at startup.
3. Never print raw secrets in logs.
4. Restore previous secret version or config if widespread.
5. Use fail-fast startup for required config.

