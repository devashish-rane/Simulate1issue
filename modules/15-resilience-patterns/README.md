# Module 15: Resilience Patterns

## Real-Life Project

Harden consumer-service calls to auth-service and other dependencies using timeouts, retry with backoff, circuit breaker, and bulkhead isolation.

This is common in microservice systems where downstreams can be slow or unavailable.

## What To Build

- Add client timeout shorter than API Gateway timeout.
- Retry only safe transient failures.
- Add exponential backoff with jitter.
- Add circuit breaker for auth introspection.
- Add bulkhead/thread-pool limit for downstream calls.
- Return structured dependency unavailable response.

## Production Concepts

- No timeout means infinite dependency coupling.
- Retrying all failures can amplify incidents.
- Circuit breakers protect callers and dependencies.
- Bulkheads stop one dependency from exhausting all request threads.
- Fallbacks must be safe and domain-aware.

## Learning-Optimized Comments To Add In Code

```java
// The dependency timeout is intentionally lower than the edge timeout so this
// service can fail fast with a controlled response.
```

```java
// Retries are limited to transient failures; auth denials are not retried.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Downstream slow | auth sleeps | consumer latency until timeout |
| Downstream down | auth unavailable | circuit opens, fast failures |
| Retry storm | retry too aggressive | dependency load increases |
| Bulkhead full | many concurrent dependency calls | controlled rejection |

## On-Call Runbook

1. Identify which dependency is slow/failing.
2. Check caller latency, dependency latency, and timeout counts.
3. Check circuit breaker state and retry rate.
4. Stop retry storm by reducing retries or opening circuit.
5. Scale only if resource saturation is proven.
6. Restore normal behavior after dependency recovers.

## Interview Talking Points

- Timeout vs retry vs circuit breaker.
- Retry amplification.
- Bulkhead isolation.
- Safe fallback design.

