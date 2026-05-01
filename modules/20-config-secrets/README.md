# Module 20: Config And Secrets

## Real-Life Project

Move runtime config and secrets out of code and into environment variables, AWS SSM Parameter Store, Secrets Manager, or AppConfig. Validate required config at startup.

This directly relates to the missing `/etc/prod-core/feature-flag.json` incident.

## What To Build

- Define required config properties with validation.
- Load non-secret config from env/SSM/AppConfig.
- Load secrets from Secrets Manager or env injected by platform.
- Fail fast at startup if required config is missing.
- Add safe feature flag fallback for non-critical config.
- Never print secret values in logs.

## Production Concepts

- Missing config should fail startup if the service cannot operate safely.
- Optional config should have safe defaults.
- Secrets need rotation and least-privilege access.
- Feature flags need ownership, expiry, and kill-switch behavior.
- Config changes can cause incidents without code deploys.

## Learning-Optimized Comments To Add In Code

```java
// Required config is validated during startup so the service fails before it
// receives traffic with an unsafe partial configuration.
```

```java
// Secret values are never logged; only the source/name and load status are safe.
```

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Missing required config | remove env var | startup/readiness failure |
| Missing optional config | remove optional flag | fallback behavior |
| Bad secret permission | remove IAM access | startup or dependency failure |
| Bad feature flag | wrong flag value | one business path changes behavior |

## On-Call Runbook

1. Check if failure started after config/secret change.
2. Check ECS task env, parameter name, and IAM permission.
3. Check startup logs for config validation.
4. For secret failures, check Secrets Manager access and rotation timeline.
5. Mitigate by reverting config, restoring secret version, or rolling back.
6. Add config validation/test so the same issue fails before deployment.

## Interview Talking Points

- Env vars vs SSM vs Secrets Manager vs AppConfig.
- Fail-fast vs fallback config.
- Secret rotation.
- Config drift and deployment safety.

