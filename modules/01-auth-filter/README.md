# Module 01: Spring Security Auth Filter

This is a runnable production-style Spring Boot lab, not only notes.

## Real-Life System

The service exposes public and protected APIs. Protected APIs require a bearer token. A request filter validates the token before controller code runs, creates an authenticated principal, and returns structured auth errors for invalid requests.

This models a common production pattern:

```text
API Gateway / ALB
  -> Spring service
  -> auth filter
  -> controller
```

## What This Project Teaches

- Authentication happens before business logic.
- `401` is different from `403` and `5xx`.
- Filters run before MVC exception handlers, so auth filter errors need deliberate JSON handling.
- Tokens must not be logged raw.
- Auth provider failure is a dependency failure, not a client auth failure.
- Request logs should contain request ID, subject, and token fingerprint.

## Run Locally

```bash
mvn -q -f modules/01-auth-filter/pom.xml test
mvn -q -f modules/01-auth-filter/pom.xml spring-boot:run
```

Service runs on:

```text
http://localhost:8091
```

## Try It

Public endpoint:

```http
GET http://localhost:8091/public/ping
```

Missing token:

```http
GET http://localhost:8091/api/me
```

Valid token:

```http
GET http://localhost:8091/api/me
Authorization: Bearer demo-user-token
```

Expired token:

```http
GET http://localhost:8091/api/me
Authorization: Bearer expired-token
```

Auth provider outage simulation:

```http
GET http://localhost:8091/api/me
Authorization: Bearer provider-down-token
```

Slow auth simulation:

```http
GET http://localhost:8091/api/me
Authorization: Bearer slow-valid-token
```

## Important Files

| File | Purpose |
| --- | --- |
| `pom.xml` | Standalone Spring Boot project dependencies |
| `src/main/java/.../SecurityConfig.java` | Stateless security chain |
| `src/main/java/.../BearerTokenAuthenticationFilter.java` | Token parsing, validation, context setup, structured auth errors |
| `src/main/java/.../InMemoryTokenIntrospectionClient.java` | Local auth-provider simulation |
| `src/main/java/.../AccountController.java` | Protected API examples |
| `src/test/java/.../AuthFilterApplicationTests.java` | Auth behavior tests |
| `RUNBOOK.md` | On-call investigation guide |

## Failure Drills

| Drill | Trigger | Expected Symptom |
| --- | --- | --- |
| Missing token | Call `/api/me` without `Authorization` | `401 AUTH_MISSING` |
| Malformed token | `Authorization: Basic abc` | `401 AUTH_MALFORMED` |
| Expired token | `Bearer expired-token` | `401 AUTH_INACTIVE` |
| Auth provider down | `Bearer provider-down-token` | `503 AUTH_PROVIDER_UNAVAILABLE` |
| Slow auth | `Bearer slow-valid-token` | `200`, but latency increases |

## On-Call Runbook

Use [RUNBOOK.md](RUNBOOK.md).

## Interview Talking Points

- Authentication vs authorization.
- Why each service still validates identity behind API Gateway.
- Why filters need their own error response strategy.
- Why raw tokens should never appear in logs.
- How auth service dependency failures should be classified.

