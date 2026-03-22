# Keyloop Scheduler

Dealership service appointment scheduling system – Resource Constrained Booking with real-time availability checks.

---

## 📋 Deliverables

- **System Design Document:** [docs/SYSTEM_DESIGN.md](docs/SYSTEM_DESIGN.md)
- **Backend Implementation:** Spring Boot REST API + PostgreSQL
- **AI Collaboration Narrative:** See section below

---

## 🚀 Run (Docker only)

From project root:

```bash
docker compose up --build
```

- **PostgreSQL** on port 5433
- **App** on port 8080
- Hibernate creates schema + DatabaseSeeder seeds data when DB is empty

---

## 🧪 Test

```bash
cd scheduler
./gradlew test
```

Test suite includes:
- `AppointmentControllerTest` – API create/get, 409 conflict, 404 not found
- `AppointmentBookingServiceTest` – Appointment creation logic, overlap handling, vehicle–customer validation
- `KeyloopSchedulerApplicationTests` – Context loads

---

## 📡 API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/appointments` | Create appointment |
| GET | `/api/appointments/{id}` | Get appointment by ID |

**Swagger UI** (when app is running):

- http://localhost:8080/swagger-ui/index.html  
- or http://localhost:8080/swagger-ui.html (redirect)

**OpenAPI JSON:** http://localhost:8080/v3/api-docs

**Example – create appointment:**

```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "vehicleId": 1,
    "dealershipId": 1,
    "serviceTypeId": 1,
    "desiredStartTime": "2026-03-25T09:00:00+07:00"
  }'
```

---

## 🤖 AI Collaboration Narrative

### Strategy for using AI

1. **Break down tasks:** Split work into parts (schema, API, service, exception, test) and ask AI to implement each.
2. **Provide context:** Share requirements, current structure, existing entities/APIs so AI stays aligned.
3. **Use AI for boilerplate:** Let AI generate JPA entities, repositories, DTOs, migrations, then refine.

### Verifying and refining AI output

1. **Run tests after each change:** Use `./gradlew test` to avoid breaking logic.
2. **Cross-check requirements:** Validate each API, validation rules, and error codes against the spec.
3. **Review code:** Pay attention to concurrency (locks, transactions) and exception handling; don’t trust AI output blindly.

### Ensuring final code quality

1. **Test coverage:** Tests for create success, overlap (409), validation (400), not found (404).
2. **Refactor:** Use a dedicated exception package and GlobalExceptionHandler instead of per-controller handlers.
3. **Documentation:** Clear System Design and README so others (and future you) understand the architecture and how to run it.
