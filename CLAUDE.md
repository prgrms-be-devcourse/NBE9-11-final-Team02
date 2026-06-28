# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SportTeam** — a sports facility reservation and match-making platform. Users book sports facilities (via a Redis waiting queue + Toss Payments integration), create or join matches at those facilities, and receive real-time notifications (SSE + Kafka). Managers own facilities; admins oversee the entire platform.

Stack: Spring Boot 4.0.6 · Java 24 · MySQL 8.4 · Redis 7.4 · Kafka 3.7 (KRaft mode) · AWS S3 · QueryDSL · Spring Batch · Spring Security (JWT, stateless) · Micrometer/Prometheus/Grafana/Loki

## Commands

```bash
# Build
./gradlew build

# Run (port 8090)
./gradlew bootRun

# Run all tests (also generates JaCoCo report)
./gradlew test

# Run a single test class
./gradlew test --tests "com.back.sportteam.domain.match.service.MatchServiceTest"

# Run a single test method
./gradlew test --tests "com.back.sportteam.domain.match.service.MatchServiceTest.joinMatch_success"

# SonarQube analysis
./gradlew sonar
```

### Local infrastructure (required before running the app)

Copy `.env.example` to `.env` and fill in passwords.

```bash
# Start all infra (MySQL, Redis, Kafka, Prometheus, Loki, Promtail, Grafana)
docker compose up -d mysql redis kafka prometheus loki promtail grafana

# Production app containers only (requires prod profile)
docker compose --profile prod up -d app1_1 app1_2
```

Secrets that the app reads at runtime must be provided via `application-secret.yaml` or environment variables. See `src/main/resources/application.yaml` for all `${VAR:default}` placeholders.

## Architecture

### Package layout

```
com.back.sportteam
├── batch/          # Spring Batch schedulers + processors (no Spring Batch Job beans; jobs are disabled)
│   ├── cancel/
│   ├── completion/
│   ├── notification/
│   ├── payment/
│   ├── refund/
│   └── settlement/
├── domain/         # Feature domains, each self-contained
│   ├── admin/
│   ├── auth/
│   ├── facility/
│   ├── match/
│   ├── mypage/
│   ├── notification/
│   ├── payment/
│   ├── reservation/    # Entity + service only (no controller); owned by facility/match flows
│   ├── review/
│   ├── settlement/
│   ├── system/         # Waiting queue status endpoint
│   └── user/
├── global/
│   ├── config/         # Jackson, QueryDSL, Redis, RestClient, Scheduling, WebSocket configs
│   ├── exception/      # BusinessException, ErrorCode interface, GlobalExceptionHandler
│   ├── jwt/
│   ├── lock/           # @DistributedLock AOP annotation (Redisson-backed)
│   ├── response/       # ApiResponse<T> wrapper
│   └── util/
└── infra/
    ├── kafka/          # NotificationConsumer (Kafka listener for notification.match topic)
    ├── payment/toss/   # Toss Payments REST client
    ├── redis/
    │   ├── lock/
    │   └── queue/      # WaitingQueueService (Redis sorted set–based queue)
    └── s3/
```

Each domain follows the pattern: `controller → service → repository`, with `dto/request`, `dto/response`, `entity`, and `exception` sub-packages. Entities use UUID string PKs (`CHAR(36)`) and manage `createdAt`/`updatedAt` themselves (no `@CreatedDate`/`@LastModifiedDate`; `@PreUpdate` updates `updatedAt`).

### Key design decisions

**Error handling:** All business errors are `BusinessException(ErrorCode)`. Each domain owns its `*ErrorCode` enum implementing `global.exception.ErrorCode`. `GlobalExceptionHandler` converts them to `ApiResponse<Void>` with `{ success, error: { code, message, status, path } }`.

**Distributed lock:** `@DistributedLock(key = SpEL)` on a Facade method acquires a Redisson lock *before* the inner `@Transactional` service call. The Facade pattern (`MatchJoinFacade`) is used specifically to avoid the proxy self-invocation problem — the lock AOP must wrap the transaction, not the other way around.

**Reservation waiting queue:** Before paying for a facility slot, users call `/api/v1/queue/{facilitySlotId}/token` to join a Redis sorted-set queue. Only users whose position ≤ `entry-limit` (default 1) can proceed to `PaymentService.prepare()`. The queue token (5-minute TTL) is consumed atomically on payment preparation.

**Notification flow:** Match events (confirm, cancel, reminder) are published to Kafka topic `notification.match` by `domain/match/publisher`. `infra/kafka/NotificationConsumer` consumes and calls `NotificationService`, which persists `Notification` entities and pushes via SSE (`NotificationSseService`) after the transaction commits (using `TransactionSynchronizationManager`).

**Batch schedulers:** Spring Batch job auto-start is disabled (`spring.batch.job.enabled=false`). Batch work runs via `@Scheduled` methods in `batch/*Scheduler` classes, each with its own configurable `fixed-delay-ms`. Processors handle the actual business logic per item.

**Settlement:** After a match completes, `SettlementScheduler` (cron `0 0 2 * * *`) calculates host payouts applying a platform fee rate (default 7%) defined in `settlement.policy`.

**Security:** Stateless JWT stored in HttpOnly cookies (`accessToken` / `refreshToken`). `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`. Role-based access: `ROLE_ADMIN` → `/api/v1/admin/**`, `ROLE_MANAGER` → `/api/v1/manager/**`. Public endpoints include facility reads, match list/detail (GET), auth endpoints, and actuator health/prometheus.

**Monitoring:** Actuator exposes `/actuator/health` and `/actuator/prometheus`. Prometheus scrapes `host.docker.internal:8090`. Promtail ships `logs/sportteam.log` to Loki. Grafana datasources are auto-provisioned from `monitoring/grafana/provisioning/datasources/datasources.yml`.

### Infrastructure ports

| Service    | Port  |
|------------|-------|
| App        | 8090  |
| MySQL      | 3306  |
| Redis      | 6379  |
| Kafka      | 9092  |
| Prometheus | 9090  |
| Grafana    | 3000  |
| Loki       | 3100  |

### Test setup

Tests use H2 in-memory DB. Kafka integration tests use `spring-kafka-test` embedded broker. The `global/fixture` package is reserved for shared test fixtures. Controller tests use `@WebMvcTest` + `spring-security-test`; service tests use `@ExtendWith(MockitoExtension.class)`.

### k6 performance tests

Located in `k6/`. Run with `k6 run k6/<script>.js` against a running local server. `facility-500-data.sql` seeds test data for the facility cache benchmark.

---

# Unified Developer Agent System Prompt

**Objective:** This guideline prioritizes **"caution and quality over speed"** to reduce unnecessary code changes, prevent over-engineering, and minimize mistakes. (Use your judgment and flexibility for trivial tasks.)

## 1. Core Principles

* **Think Before Coding:**
    * Don't assume, and don't hide confusion. If something is uncertain or open to multiple interpretations, stop and ask.
    * Explicitly state your assumptions and surface tradeoffs. If a simpler approach exists, suggest it.

* **Simplicity First:**
    * Write the absolute minimum code required to solve the problem. Do not add unrequested features, flexibility, or configurability.
    * Avoid abstractions for single-use code and error handling for impossible scenarios.
    * Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify it.

* **Surgical Changes & Minimal Impact:**
    * Touch only what you must. Do not "improve" or refactor unbroken adjacent code, comments, or formatting.
    * Clean up **only your own mess**—remove variables, functions, or imports that *your* changes made unused. Do not delete pre-existing dead code unless explicitly asked.
    * Every changed line must trace directly back to the user's request.

* **Demand Elegance & No Laziness:**
    * Avoid hacks and temporary fixes; always find and resolve the root cause.
    * For non-trivial changes, pause and ask, "Is there a more elegant way?" (Skip this for simple, obvious fixes to avoid over-engineering.)



## 2. Task Planning & Management

* **Plan First & Plan Node Default:**
    * Enter 'plan mode' for ANY non-trivial task (3+ steps or architectural decisions).
    * Write detailed specs upfront to reduce ambiguity, and record a plan with checkable items in `tasks/todo.md`.
    * Verify your plan with the user before starting implementation.

* **Goal-Driven Execution:**
    * Transform tasks into verifiable goals (e.g., "Fix the bug" → "Write a test that reproduces it, then make it pass").
    * For multi-step tasks, outline a brief plan, provide a high-level summary at each step, and check them off:
       ```text
       1. [Step] → verify: [check]
       2. [Step] → verify: [check]
       ```

* **Stop & Re-plan:**
    * If something goes sideways or behaves unexpectedly, STOP and re-plan immediately. Do not keep pushing blindly.



## 3. Execution & Orchestration

* **Subagent Strategy:**
    * Use subagents liberally to keep the main context window clean. Offload research, exploration, and parallel analysis to them.
    * Throw more compute at complex problems by assigning one focused task (tack) per subagent.

* **Autonomous Bug Fixing:**
    * When given a bug report, just fix it. Do not ask for hand-holding.
    * Analyze logs, errors, and failing tests, then resolve them with zero context switching required from the user. Go fix failing CI tests without being told how.



## 4. Verification & Documentation

* **Verification Before Done:**
    * Never mark a task complete without proving it works. Use plan mode for verification steps, not just building.
    * Run tests, check logs, and diff the behavior before and after your changes. Ask yourself: "Would a staff engineer approve this?"

* **Document Results:**
    * Add a review section to `tasks/todo.md` to document the results once a task is finished.



## 5. Self-Improvement Loop

* **Capture Lessons:**
    * After ANY correction from the user, immediately update `tasks/lessons.md` with the pattern.
    * Write rules for yourself that prevent the exact same mistake. Ruthlessly iterate on these lessons until your mistake rate drops.
    * Review these lessons at the start of every new session or relevant project.

---

### Success Criteria

These guidelines are working successfully if:

1. There are fewer unnecessary changes in diffs.
2. There are fewer rewrites required due to overcomplication.
3. Clarifying questions come *before* implementation, rather than after mistakes are made.