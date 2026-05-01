# Production Backend Learning Modules

Use these modules as hands-on SDE-2/SDE-3 production labs. Each module is a small real-life system slice with implementation goals, observability expectations, failure drills, and an on-call runbook.

The goal is not only to make code work. The goal is to learn how production systems fail, how teams operate them, and how to explain the tradeoffs in interviews.

Each module is now a runnable Spring Boot project with:

- `pom.xml`
- `src/main`
- `src/test`
- `RUNBOOK.md`

Run all module tests:

```bash
mvn -q -f modules/pom.xml test
```

Run one module:

```bash
mvn -q -f modules/01-auth-filter/pom.xml spring-boot:run
```

| # | Module | Project |
| ---: | --- | --- |
| 01 | [Spring Security Auth Filter](01-auth-filter/README.md) | Request authentication gateway inside the service |
| 02 | [Method-Level RBAC](02-rbac/README.md) | Role and permission enforcement for admin/user APIs |
| 03 | [Tenant Context Filter](03-tenant-context/README.md) | Tenant-aware request context and validation |
| 04 | [Tenant Data Isolation](04-tenant-data-isolation/README.md) | Prevent cross-tenant data access |
| 05 | [Global Exception Handler](05-global-exception-handler/README.md) | Production error model and failure classification |
| 06 | [Request/Response DTOs](06-dtos/README.md) | Stable public API contracts |
| 07 | [Bean Validation](07-bean-validation/README.md) | Input validation and safe client errors |
| 08 | [JPA Transactions](08-transactions/README.md) | Atomic writes and rollback behavior |
| 09 | [N+1 Query Debugging](09-n-plus-one/README.md) | Query explosion detection and fixes |
| 10 | [Pagination With Indexes](10-pagination-indexes/README.md) | Scalable list APIs |
| 11 | [Optimistic Locking](11-optimistic-locking/README.md) | Concurrent update conflict handling |
| 12 | [Idempotency Keys](12-idempotency/README.md) | Safe client retries |
| 13 | [Outbox Pattern](13-outbox/README.md) | Reliable event publishing after DB writes |
| 14 | [Queue Consumer Retries](14-queue-retries/README.md) | SQS/Kafka retry and DLQ operations |
| 15 | [Resilience Patterns](15-resilience-patterns/README.md) | Timeout, retry, circuit breaker, bulkhead |
| 16 | [Connection Pool Tuning](16-connection-pool/README.md) | HikariCP and DB saturation debugging |
| 17 | [Actuator Health Design](17-health-design/README.md) | Liveness, readiness, and business health |
| 18 | [Structured Logging](18-structured-logging/README.md) | JSON logs with trace, span, tenant, user, error code |
| 19 | [Micrometer Metrics](19-micrometer-metrics/README.md) | Custom technical and business metrics |
| 20 | [Config And Secrets](20-config-secrets/README.md) | Runtime config, secrets, and fail-fast startup |

## Recommended Build Order

1. Auth filter
2. RBAC
3. Tenant context
4. Tenant data isolation
5. Global error model
6. DTO validation
7. Pagination
8. Transactions
9. Idempotency
10. Async queue processing
11. Observability
12. Deployment safety

## Per-Module Definition Of Done

- The feature works on the happy path.
- Invalid input produces a deliberate error response.
- Logs identify the service, endpoint, tenant/user when relevant, and error code.
- Metrics show request count, latency, success/failure rate, and dependency behavior.
- At least one failure drill can be triggered from UI or HTTP.
- The runbook explains what to check first, how to confirm impact, and how to mitigate.
