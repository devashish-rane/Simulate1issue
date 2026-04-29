# Prod Core On-Call Lab

Use this repo as an SDE-2/SDE-3 production-debugging lab. The UI gives you symptoms only. Your job is to find the failing layer using AWS evidence: CloudFront, API Gateway, ALB target health, ECS events, ECS logs, CloudWatch metrics, X-Ray/Application Signals, SQS, and DynamoDB.

UI:

```text
https://djc4d23uxmtt5.cloudfront.net
```

API base:

```text
https://djc4d23uxmtt5.cloudfront.net
```

Practice rule: trigger one incident, write down the request ID / trace ID / job ID, then investigate before reading source code.

## Incident Deck

| ID | Trigger | Symptom to observe |
| --- | --- | --- |
| INC-01 | `GET /api/lab/feature` | Health check is UP, but this action returns a server error. |
| INC-02 | `4x GET /api/lab/inventory` | Several calls succeed, then one fails with a service-style error. |
| INC-03 | `10x GET /api/lab/profile` | Most calls are fast, but one call may be much slower. |
| INC-04 | `3x parallel GET /api/lab/aggregate?durationMs=3500` | Latency increases and ECS CPU should move after the trigger. |
| INC-05 | `5x GET /api/lab/cache?mb=3` | Repeated calls report increasing memory-related values. |
| INC-06 | `3x parallel GET /api/lab/reconcile` | Parallel calls complete slowly instead of all finishing at once. |
| INC-07 | `GET /api/lab/export?kb=7000` | The action returns a large body and takes longer than normal. |
| INC-08 | `GET /api/lab/session` | The call is rejected with an authorization-style response. |
| INC-09 | `GET /api/lab/quote?plan=starter&items=1` | The call fails as a client-side/business validation response. |
| INC-10 | `GET /api/lab/timeout` | The request waits and eventually fails at the edge. |
| INC-11 | UI incident button | Job creation returns an authorization failure. |
| INC-12 | UI incident button | Job creation fails before the worker gets involved. |
| INC-13 | UI incident button | The job is accepted but does not complete immediately. |
| INC-14 | UI incident button | The job is accepted, then later shows retry/final status movement. |
| INC-15 | UI incident button | The job is accepted but should eventually stop progressing normally. |
| INC-16 | UI incident button | Many jobs are accepted quickly, then completion lags behind. |
| INC-17 | UI incident button | Token creation is rejected before consumer-service is called. |
| INC-18 | `GET /api/lab/mismatch` | HTTP status looks successful, but the body says the operation failed. |
| INC-19 | `2x GET /api/lab/checkout` | A repeated business action gives different results within seconds. |
| INC-20 | `GET /api/lab/not-a-real-route` | The request reaches the system but no application route handles it. |

## Issue List

Implemented in UI:

- [x] 01. Green health, one business action fails
- [x] 02. Intermittent 503 after repeated calls
- [x] 03. Tail latency spike
- [x] 04. CPU rises after one action
- [x] 05. Memory trend changes
- [x] 06. Concurrent requests slow each other
- [x] 07. Large response is slow
- [x] 08. Expected 401 path
- [x] 09. Expected 4xx path
- [x] 10. Gateway timeout
- [x] 11. Job create is rejected
- [x] 12. Job create missing credentials
- [x] 13. Job stays unfinished at first
- [x] 14. Job changes state after retries
- [x] 15. Failure queue exercise
- [x] 16. Queue backlog exercise
- [x] 17. Token request rejected
- [x] 18. Success status with failed body
- [x] 19. Alternating checkout failure
- [x] 20. Route not found

Next implementation backlog:

- [ ] 21. ECS task unhealthy
- [ ] 22. Task crash loop
- [ ] 23. Bad health check design
- [ ] 24. SQS visibility timeout duplicate processing
- [ ] 25. Partial DB/SQS failure
- [ ] 26. Missing correlation ID
- [ ] 27. Async trace gap
- [ ] 28. Alarm missing
- [ ] 29. Alarm too noisy
- [ ] 30. IAM permission failure

## What To Capture

For every incident, record only evidence:

- HTTP status
- API request ID
- X-Ray trace ID
- job ID, when present
- timestamp
- service log group checked
- dashboard graph checked
- suspected layer
- proof
- fix or rollback action

## Direct Health Check

```http
GET https://djc4d23uxmtt5.cloudfront.net/actuator/health
```

Expected symptom:

```text
HTTP 200 with status UP
```
