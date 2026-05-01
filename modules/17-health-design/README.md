# Module 17: Actuator Health Design

## Real-Life Project

Design health endpoints that correctly separate process liveness, traffic readiness, dependency health, and business canary health.

This directly connects to your current incidents where `/actuator/health` is green but business endpoints fail.

## What To Build

- `/actuator/health/liveness`: process is alive.
- `/actuator/health/readiness`: service can receive traffic.
- Dependency indicators for DynamoDB, SQS, and auth service.
- Optional business health endpoint for a safe read path.
- Configure ALB to use readiness, not deep business checks.
- Add synthetic/canary plan for important flows.

## Production Concepts

- Liveness should not fail because a downstream dependency is down.
- Readiness controls whether load balancer sends traffic.
- Deep dependency checks can cause cascading restarts.
- Business canaries catch what health checks miss.
- Health endpoint cost and timeout must be controlled.

## Learning-Optimized Comments To Add In Code

```java
// Liveness answers "should the container be restarted?" not "is every
// dependency healthy?"
```

```java
// Readiness answers "should this instance receive traffic right now?"
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| App alive, feature broken | missing config | health green, endpoint fails |
| Dependency down | Dynamo/SQS failure | readiness or dependency health changes |
| Bad health check | deep check too slow | task marked unhealthy |
| Startup delay | slow boot | grace period matters |

## On-Call Runbook

1. Check ALB target health and ECS service events.
2. Check liveness vs readiness state.
3. Check if health check is failing due to app process or dependency.
4. If health check is too strict, prevent restart loop and roll back.
5. If dependency is down, avoid restarting healthy app instances endlessly.
6. Add business canary for user-critical flows.

## Interview Talking Points

- Liveness vs readiness.
- Why health checks should be cheap.
- Dangers of deep dependency health checks.
- Synthetic canaries vs health endpoints.

