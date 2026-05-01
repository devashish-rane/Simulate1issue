# On-Call Runbook: Resilience Patterns Lab

## Fast Checks

1. Identify failing dependency.
2. Check timeout count, retry rate, and circuit state.
3. Stop retry amplification.
4. Confirm caller timeout is lower than edge timeout.
5. Restore dependency before closing circuit.

