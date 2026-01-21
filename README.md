# Order API Management (ANZ Coding Challenge)

Spring Boot service that manages Orders and supports:
- Create Order
- Get Order by ID
- Update Order status with valid transitions
- Search Orders with filters + pagination
- Send notifications on order events (pluggable channels; webhook included)
- Consistent error responses

## Tech Stack
- Java 21
- Spring Boot (Boot 4.x)
- Spring Web, Validation
- Spring Data JPA
- H2 Database (file for local, in-memory for tests)
- Spring Security (Basic Auth)
- Spring Retry (for webhook retries)
- JUnit 5 + Spring Boot Test (+ WireMock for notification tests)

---

## Quick Start

### Prerequisites
- Java 21 installed
- Maven wrapper included (or Maven installed)

### Run locally
**Mac/Linux**
```bash
./mvnw spring-boot:run
```

**Windows (PowerShell)**
```powershell
.\mvnw.cmd spring-boot:run
```

App starts on:
- `http://localhost:8080`

### Authentication
API is protected with **Basic Auth**.

Default credentials (see `application.yml` or `SecurityConfig`):
- Username: `user`
- Password: `password`

Example header:
```bash
-H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

---

## Configuration

### application.yml (local)
- H2 file DB (example):
    - `jdbc:h2:file:./data/orders;MODE=PostgreSQL;AUTO_SERVER=TRUE`
- H2 console (if enabled):
    - `http://localhost:8080/h2-console`

### Notifications
Notifications are configurable via `notification.*`

Example:
```yaml
notification:
  enabled-channels: [webhook]
  webhook:
    base-url: http://localhost:8089
    path: /notify
  retry:
    max-attempts: 3
    initial-delay-ms: 200
    multiplier: 2.0
```

- `enabled-channels`: list of channel names to enable (e.g. `webhook`)
- `webhook.base-url` + `webhook.path`: target endpoint
- Retry settings are configurable; webhook uses Spring Retry.

> Tests typically override `webhook.base-url` to WireMock.

---

## API

> Endpoints/DTOs may vary slightly depending on the implementation.

### 1) Create Order
`POST /orders`

Request:
```json
{
  "customerId": "cust-123"
}
```

cURL:
```bash
curl -i -u user:password \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-123"}' \
  http://localhost:8080/orders
```

Response `201 Created` (example):
```json
{
  "id": "UUID",
  "status": "CREATED",
  "customerId": "cust-123",
  "createdAt": "2026-01-20T04:19:53.691Z",
  "updatedAt": "2026-01-20T04:19:53.691Z"
}
```

### 2) Get Order by ID
`GET /orders/{id}`

```bash
curl -i -u user:password http://localhost:8080/orders/<uuid>
```

Response `200 OK` returns the order DTO.

### 3) Update Order Status
`PATCH /orders/{id}/status`

Request:
```json
{ "status": "COMPLETED" }
```

```bash
curl -i -u user:password \
  -H "Content-Type: application/json" \
  -X PATCH \
  -d '{"status":"COMPLETED"}' \
  http://localhost:8080/orders/<uuid>/status
```

#### Status rules / transitions
Valid statuses:
- `CREATED`
- `COMPLETED`
- `CANCELLED`

Typical allowed transitions:
- `CREATED -> COMPLETED`
- `CREATED -> CANCELLED`
- `COMPLETED -> (no transitions)`
- `CANCELLED -> (no transitions)`

Invalid transitions return `409 Conflict`.

### 4) Search Orders (filters + pagination)
`GET /orders`

Query parameters:
- `status` (optional) e.g. `CREATED`
- `createdFrom` (optional, ISO-8601) e.g. `2026-01-01T00:00:00Z`
- `createdTo` (optional, ISO-8601)
- `page` (optional, default 0)
- `size` (optional, default 20)

Example:
```bash
curl -i -u user:password \
  "http://localhost:8080/orders?status=CREATED&page=0&size=10"
```

Response:
- If returning Spring `Page` directly, JSON typically includes `content`, `totalElements`, etc.
- If using a custom paging DTO, JSON might include `items`, `page`, `size`, `totalElements`, `totalPages`.

> Keep tests aligned with the chosen response shape (e.g. `$.content` vs `$.items`).

---

## Error Response Format

All errors return a consistent JSON body:

```json
{
  "timestamp": "2026-01-20T04:19:53.691Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/orders",
  "details": {
    "fieldErrors": {
      "customerId": "must not be blank"
    }
  }
}
```

Common codes:
- `400 Bad Request` — validation errors, malformed JSON, type mismatch
- `404 Not Found` — order not found
- `409 Conflict` — invalid status transition
- `500 Internal Server Error` — unexpected errors

---

## Notifications

### Behavior
- On `ORDER_CREATED` and `ORDER_STATUS_CHANGED`, the system attempts to notify enabled channels.
- The webhook implementation posts a JSON message to the configured URL.
- Webhook failures are retried (Spring Retry).
- Final failure is swallowed so the main API call still succeeds (notification is best-effort).

### Payload (example)
```json
{
  "type": "ORDER_CREATED",
  "payload": {
    "orderId": "UUID"
  }
}
```

---

## Testing

### Run all tests
**Mac/Linux**
```bash
./mvnw test
```

**Windows (PowerShell)**
```powershell
.\mvnw.cmd test
```

### Test profile
Integration tests commonly use:
- `application-test.yml` (H2 in-memory, test-specific notification config)

If you want tests to always load `application-test.yml`, annotate tests with:
```java
@ActiveProfiles("test")
```
(or configure Surefire to set `spring.profiles.active=test`).

---

## Project Structure (high level)
- `api/` – controllers + request/response DTOs
- `domain/` – Order entity, status enum, domain exceptions
- `service/` – business logic, status transitions, publishing events
- `repository/` – Spring Data JPA repository
- `notification/` – routing + channels (webhook), listener for order events
- `error/` – global exception handling + API error model
- `config/` – security config

---

## Notes / Design Decisions
- Separation of concerns: controller → service → repository; notifications handled separately.
- Best-effort notifications: order operations should not fail due to external notification outage.
- Resilience: webhook uses retry with backoff.
- Consistency: standardized error schema across failure modes.
- Testability: notifications are integration-tested using WireMock.

---

## Possible Enhancements (beyond task scope)
- Add idempotency for create order (client-provided idempotency key)
- Outbox pattern / async queue for guaranteed notification delivery
- Stable paging response contract (DTO) to avoid `PageImpl` JSON instability warnings
- OpenAPI/Swagger documentation
- Additional notification channels (Email/SMS) behind the same interface
