# System Design Document - Keyloop Scheduler

## 1. Overview

**Keyloop Scheduler** is a dealership service appointment scheduling system. It lets users book appointments based on available resources (Service Bay + Technician) with real-time availability checks.

---

## 2. System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                    │
│  (cURL / Postman / Frontend mock / API Consumer)                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY LAYER                                  │
│  REST API (Spring Web MVC) - /api/appointments                           │
│  • POST /api/appointments  → Create appointment                          │
│  • GET  /api/appointments/{id} → Get appointment                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     APPLICATION LAYER                                     │
│  ┌─────────────────┐  ┌──────────────────────┐  ┌─────────────────────┐ │
│  │  Controller     │  │  BookingService      │  │  GlobalException    │ │
│  │  (Validation)   │──│  (Business Logic)    │──│  Handler (Logging)  │ │
│  └─────────────────┘  └──────────────────────┘  └─────────────────────┘ │
│                                │                                          │
│                                ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │  Repositories (JPA) - Customer, Vehicle, Dealership, ServiceType,   │ │
│  │  ServiceBay, Technician, Appointment                                │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        DATA LAYER                                         │
│  PostgreSQL (persistent storage)                                         │
│  • Hibernate JPA + pessimistic locking for concurrency                   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     OBSERVABILITY                                         │
│  Spring Boot Actuator (/actuator/health, /actuator/metrics)              │
│  SLF4J + Logback (structured logging)                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Roles

| Component | Role |
|-----------|------|
| **AppointmentController** | Handles HTTP requests, validates input, calls service, returns response |
| **AppointmentBookingService** | Business logic: find available bay + technician, create appointment, handle race conditions |
| **Repositories** | DB queries, available resource lookup, pessimistic locking |
| **GlobalExceptionHandler** | Centralized exception handling, error logging, standard error responses |
| **DatabaseSeeder** | Seeds sample data when DB is empty (development) |
| **PostgreSQL** | Persistent storage, ACID, supports concurrent transactions |

---

## 4. Data Flow

### 4.1 Create Appointment (POST /api/appointments)

```
1. Client sends JSON: {customerId, vehicleId, dealershipId, serviceTypeId, desiredStartTime}
2. Controller validates (@Valid) → calls BookingService.createAppointment()
3. Service:
   a. Load Customer, Vehicle, Dealership, ServiceType (validate existence)
   b. Validate: Vehicle belongs to Customer
   c. Compute endTime = desiredStartTime + durationMinutes
   d. Query available ServiceBay (dealership + no overlap)
   e. Query available Technician (dealership + no overlap)
   f. Loop: pick first available (bay, technician) pair
   g. Pessimistic lock bay + technician → double-check overlap → INSERT appointment
   h. Return AppointmentResponse
4. Controller returns 201 Created + JSON response
```

### 4.2 Get Appointment (GET /api/appointments/{id})

```
1. Client sends GET with id
2. Controller calls BookingService.getAppointment(id)
3. Service: Repository.findById() → orElseThrow AppointmentNotFoundException
4. Map entity → AppointmentResponse
5. Controller returns 200 OK + JSON
```

---

## 5. Technology Choices & Rationale

| Technology | Rationale |
|------------|-----------|
| **Spring Boot 3.4** | Popular Java framework with JPA, Validation, Actuator built-in |
| **PostgreSQL** | ACID, supports concurrent access, production-ready |
| **Hibernate JPA** | Standard ORM, pessimistic locking for booking, maintainable |
| **Gradle** | Modern build tool, solid dependency management |
| **Docker Compose** | Run full stack (app + DB) with one command |
| **H2 (test)** | In-memory DB for fast unit/integration tests |
| **JUnit 5** | Standard Java testing framework |

---

## 6. Observability Strategy

| Mechanism | Description |
|-----------|-------------|
| **Logging** | SLF4J + Logback. GlobalExceptionHandler logs WARN/ERROR. Default Spring Boot app logging. |
| **Metrics** | Spring Boot Actuator: `/actuator/health`, `/actuator/metrics` |
| **Health Check** | `/actuator/health` – used for Docker healthcheck, load balancer |
| **Tracing** | Not yet integrated (can add Micrometer Tracing + OpenTelemetry for scaling) |

---

## 7. Scalability, Performance, Reliability, Maintainability

| Criterion | Implementation |
|-----------|----------------|
| **Scalability** | Stateless API → easy horizontal scaling. DB connection pooling (HikariCP). |
| **Performance** | Index on (service_bay_id, start_time, end_time) and (technician_id, start_time, end_time). Pessimistic lock only on needed rows. |
| **Reliability** | Transactions (@Transactional). Pessimistic lock avoids double-booking. GlobalExceptionHandler prevents exception leaks. |
| **Maintainability** | Clear packages (api, domain, service, repository, exception). Dedicated exception handler. |
| **Observability** | Logging + Actuator. Easy to extend with metrics/tracing. |

---

## 8. Concurrent Request Handling (Two Requests at Same Time)

When two clients send `POST /api/appointments` simultaneously for the same time slot and dealership, the following sequence occurs:

### Step-by-step timeline (Request A and Request B)

| Step | Request A | Request B | DB state |
|------|-----------|-----------|----------|
| 1 | Starts transaction, loads Customer/Vehicle/Dealership/ServiceType | Starts transaction, loads same entities | Both see initial state |
| 2 | `findAvailableServiceBayIds` → [1, 2, 3] | `findAvailableServiceBayIds` → [1, 2, 3] | No locks yet |
| 3 | `findAvailableTechnicianIds` → [1, 2, 3] | `findAvailableTechnicianIds` → [1, 2, 3] | No locks yet |
| 4 | `findByIdForUpdate(bayId=1)` → **acquires lock** on Bay 1 row | — | Bay 1 locked by A |
| 5 | — | `findByIdForUpdate(bayId=1)` → **blocks** (waits) | B waits for A to release |
| 6 | `countOverlappingForServiceBay(1)` = 0 | (still waiting) | — |
| 7 | `findByIdForUpdate(techId=1)` → **acquires lock** on Tech 1 | (still waiting) | Tech 1 locked by A |
| 8 | `countOverlappingForTechnician(1)` = 0 | (still waiting) | — |
| 9 | `save(appointment)` → INSERT | (still waiting) | New row in `appointment` |
| 10 | **commit** → releases Bay 1 + Tech 1 locks | (resumes) | Locks freed |
| 11 | Returns 201 Created | `findByIdForUpdate(bayId=1)` → **acquires lock** | Bay 1 locked by B |
| 12 | — | `countOverlappingForServiceBay(1)` **> 0** (A's appointment exists) | B sees overlap |
| 13 | — | `continue` → tries Bay 2 | — |
| 14 | — | Locks Bay 2 + Tech 2, double-check, INSERT, commit | B books different slot |
| 15 | — | Returns 201 Created (or 409 if no other slot) | — |

### Key points

- **Blocking, not failing:** Request B does not error; it waits at the database level until A releases the lock.
- **Double-check:** After acquiring the lock, `countOverlappingForServiceBay` / `countOverlappingForTechnician` re-verify availability. If A already booked the slot, B skips that (bay, technician) pair and tries the next.
- **No double-booking:** Only one transaction can hold the lock on a given Bay/Technician row at a time; the first to lock wins for that resource.
- **Order:** PostgreSQL decides which transaction gets the lock first (typically first to request). Others block and retry.

---

## 9. Future Extensions – Redis

The current design relies on PostgreSQL row-level locking. For higher scale or different deployment models, Redis can be introduced as an extension:

### Possible uses of Redis

| Use case | Description | Benefit |
|----------|-------------|---------|
| **Distributed lock** | Lock Bay/Technician slots in Redis before hitting DB | Reduce DB lock contention when many app instances share one DB |
| **Availability cache** | Cache available slots per dealership/time window | Faster responses for “check availability” or listing endpoints |
| **Rate limiting** | Limit requests per user/IP | Protect API from abuse |
| **Session / temp reservation** | Hold a slot for N minutes while user completes booking | Reduce cart-abandonment style conflicts |

### Example: Redis-based distributed lock (future)

```
1. Before findByIdForUpdate: try Redis SET key="lock:bay:{id}:{start}:{end}" NX EX 30
2. If lock acquired → proceed to DB lock + insert
3. If not acquired → return 409 or retry with backoff
4. On commit/rollback: DEL key
```

### Trade-offs

- **Pros:** Lower DB load under high concurrency; supports multi-region deployments.
- **Cons:** Extra component to run and maintain; need to handle Redis failures; potential inconsistency if lock TTL is misconfigured.
- **Current stance:** PostgreSQL locking is sufficient for single-region, moderate traffic. Redis is a future option when scaling out or adding new features (caching, temp holds, etc.).

---

## 10. AI (GenAI) Usage in Design

- **Design phase:** Use AI to brainstorm architecture, suggest component diagrams, and spot gaps.
- **Technology selection:** Ask AI to compare JPA vs raw SQL, PostgreSQL vs MySQL for this booking use case.
- **Data flow:** AI helps describe the flow from request to DB and back.
- **Review:** Always cross-check AI output against best practices (Spring, JPA, concurrency) and adjust to context.
