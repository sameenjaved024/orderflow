# OrderFlow

A production-grade e-commerce order processing pipeline built with Java 21 and Spring Boot 3.
Demonstrates distributed systems engineering across four microservices connected via Apache Kafka and RabbitMQ.

>  **Status: In Progress** — order-service and fulfillment-service complete. notification-service and ai-service under active development.

---

## Architecture

```
[Client]
   │
   ▼  REST POST /api/v1/orders
┌─────────────────────┐
│    order-service    │  :8081
│  Spring Boot 3      │
│  PostgreSQL         │
└─────────┬───────────┘
          │
          │  Kafka topic: order.created
          ▼
┌─────────────────────┐
│ fulfillment-service │  :8082
│  Spring Boot 3      │
│  PostgreSQL         │
└─────────┬───────────┘
          │
          │  RabbitMQ exchange: orderflow.exchange
          ▼
┌─────────────────────┐
│notification-service │  :8083   In Progress
│  Spring Boot 3      │
│  Email / SMS        │
└─────────────────────┘

┌─────────────────────┐
│     ai-service      │  :8084   In Progress
│  OpenAI GPT-4o-mini │
│  Anomaly Detection  │
└─────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Status |
|---|---|---|
| Language | Java 21, Spring Boot 3.3.5 | ✅ |
| Messaging | Apache Kafka | ✅ |
| Messaging | RabbitMQ | ✅ |
| Database | PostgreSQL 15 | ✅ |
| ORM | Hibernate / Spring Data JPA | ✅ |
| Mapping | MapStruct | ✅ |
| Infrastructure | Docker, Docker Compose | ✅ |
| Secrets | HashiCorp Vault |  Planned |
| Orchestration | Kubernetes, Helm |  Planned |
| Infrastructure as Code | Terraform |  Planned |
| Observability | Datadog APM |  Planned |
| AI Integration | OpenAI GPT-4o-mini |  Planned |
| Testing | JUnit 5, Mockito, Testcontainers | ✅ |
| Build | Maven 3.9.x (multi-module) | ✅ |

---

## Services

### ✅ order-service — Complete

Accepts orders via REST API, validates input, persists to PostgreSQL, and publishes `OrderCreatedEvent` to Kafka.

**Key design decisions:**
- DB write happens **before** Kafka publish — if Kafka is unavailable the order is still saved and can be replayed
- Kafka failure is logged silently — never thrown to the caller
- `orderId` used as Kafka message key for correct partition routing
- RFC 7807 `ProblemDetail` for consistent error responses
- MapStruct for compile-time DTO mapping — zero runtime reflection

**Endpoints:**
```
POST   /api/v1/orders                    Create a new order
GET    /api/v1/orders/{orderId}          Get order by ID
GET    /api/v1/orders/customer/{id}      Get all orders for a customer
GET    /actuator/health                  Health check
```

**Tests:** 12 tests — 6 service unit tests, 6 controller tests. All passing.

---

### ✅ fulfillment-service — Complete

Consumes `order.created` events from Kafka, processes fulfilment, and publishes `FulfillmentCompletedEvent` to RabbitMQ.

**Key design decisions:**
- **Manual Kafka ACK** — offset committed only after DB write and RabbitMQ publish both succeed. Zero message loss on crash.
- **Idempotency check** — duplicate Kafka deliveries are detected via `existsByOrderId` and safely skipped
- **Dead-letter queue** — failed RabbitMQ messages routed to DLQ instead of being dropped. Inspectable and replayable from management UI.
- DB write before RabbitMQ publish — same consistency guarantee as order-service
- `OrderEventConsumer` and `FulfillmentService` are separate classes — Kafka transport concern separated from business logic

**Tests:** 3 service unit tests. All passing.

---

### notification-service — In Progress

Consumes `FulfillmentCompletedEvent` from RabbitMQ and dispatches email and SMS notifications.

**Planned design:**
- Strategy pattern — each notification channel (`EmailNotificationTemplate`, `SmsNotificationTemplate`) implements `NotificationTemplate`
- Adding a new channel (push, WhatsApp) requires zero changes to existing code — just a new `@Component`
- RabbitMQ fanout to separate email and SMS queues

---

###  ai-service — In Progress

AI-powered order anomaly detection using OpenAI GPT-4o-mini.

**Planned features:**
- Structured JSON prompt — returns `anomalyScore`, `riskLevel`, and `reason`
- Graceful degradation — returns LOW risk fallback if OpenAI is unavailable
- AI-assisted test generation and notification content generation

---

## Local Development

### Prerequisites

- Java 21 (Amazon Corretto recommended)
- Maven 3.9.x
- Docker Desktop or Colima

### 1. Start Infrastructure

```bash
docker compose up -d
```

Verify all containers are healthy:

```bash
docker compose ps
```

### 2. Build All Modules

```bash
mvn clean install -DskipTests
```

### 3. Run Services

Start each service from IntelliJ using the Spring Boot run configurations,
or from terminal:

```bash
# Terminal 1
cd order-service && mvn spring-boot:run

# Terminal 2
cd fulfillment-service && mvn spring-boot:run
```

### 4. Create a Test Order

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":  "cust-001",
    "productId":   "prod-001",
    "quantity":    2,
    "totalAmount": 49.99
  }'
```

Expected response — `201 Created`:

```json
{
    "id":           "uuid-here",
    "customerId":   "cust-001",
    "productId":    "prod-001",
    "quantity":     2,
    "totalAmount":  49.99,
    "status":       "PENDING",
    "createdAt":    "2026-04-30T...",
    "updatedAt":    "2026-04-30T..."
}
```

### 5. Verify the Pipeline

After creating an order, verify each stage:

| Stage | How to verify |
|---|---|
| Order saved to DB | `GET /api/v1/orders/{id}` returns the order |
| Kafka event published | Kafka UI → Topics → `order.created` → Messages |
| Fulfillment processed | fulfillment-service console shows dispatch logs |
| RabbitMQ event published | RabbitMQ UI → Queues → `notification.fulfillment` |

---

## Local Dashboard URLs

| Service | URL | Credentials |
|---|---|---|
| Kafka UI | http://localhost:8090 | None |
| RabbitMQ Management | http://localhost:15672 | orderflow / orderflow_pass |
| Vault UI | http://localhost:8200 | Token: dev-root-token |
| order-service health | http://localhost:8081/actuator/health | None |
| fulfillment-service health | http://localhost:8082/actuator/health | None |

---

## Running Tests

```bash
# All tests across all modules
mvn test

# Single service
mvn test -pl order-service
mvn test -pl fulfillment-service
```

Current test results:

```
order-service       — Tests run: 12, Failures: 0 ✅
fulfillment-service — Tests run: 3,  Failures: 0 ✅
```

---

## Project Structure

```
orderflow/
├── order-service/                     REST API + Kafka producer
│   └── src/main/java/com/orderflow/order/
│       ├── config/KafkaProducerConfig.java
│       ├── controller/OrderController.java
│       ├── domain/Order.java
│       ├── dto/CreateOrderRequest.java
│       ├── dto/OrderResponse.java
│       ├── event/OrderCreatedEvent.java
│       ├── exception/GlobalExceptionHandler.java
│       ├── mapper/OrderMapper.java
│       ├── repository/OrderRepository.java
│       └── service/OrderService.java
│
├── fulfillment-service/               Kafka consumer + RabbitMQ producer
│   └── src/main/java/com/orderflow/fulfillment/
│       ├── config/KafkaConsumerConfig.java
│       ├── config/RabbitMQConfig.java
│       ├── consumer/OrderEventConsumer.java
│       ├── domain/Fulfillment.java
│       ├── event/OrderCreatedEvent.java
│       ├── event/FulfillmentCompletedEvent.java
│       ├── repository/FulfillmentRepository.java
│       └── service/FulfillmentService.java
│
├── notification-service/              RabbitMQ consumer  
├── ai-service/                        AI anomaly detection  
├── infrastructure/
│   └── postgres/init.sql
├── docker-compose.yml
└── pom.xml
```

---

## Key Design Decisions

### DB-first pattern
Orders and fulfillments are always persisted to PostgreSQL **before** publishing to Kafka or RabbitMQ.
If the message broker is unavailable, the record still exists and can be replayed.
Publishing first and failing the DB write creates phantom events with no corresponding record — much harder to recover from.

### Manual Kafka ACK
`ENABLE_AUTO_COMMIT=false` with `AckMode.MANUAL_IMMEDIATE`.
The consumer ACKs only after both the DB write and RabbitMQ publish succeed.
If anything fails, Kafka redelivers — no order is ever silently dropped.

### Idempotency check in fulfillment-service
Before processing any event, the service checks whether a fulfillment already exists for that `orderId`.
Duplicate Kafka deliveries are safely skipped with a log warning.
This prevents one order from creating two fulfillment records and two notifications.

### Dead-letter queue
Every RabbitMQ queue has an `x-dead-letter-exchange` configured.
Messages that fail after retries go to the DLQ instead of being dropped.
They can be inspected and replayed from the RabbitMQ management UI.

### Strategy pattern for notifications
`NotificationService` holds a `List<NotificationTemplate>` injected by Spring.
Adding a new notification channel requires only a new `@Component` implementing `NotificationTemplate`.
Zero changes to any existing code.

---

## Roadmap

- [x] order-service — REST API, Kafka producer, PostgreSQL, TDD
- [x] fulfillment-service — Kafka consumer, RabbitMQ producer, idempotency, TDD
- [ ] notification-service — RabbitMQ consumer, Strategy pattern, email/SMS
- [ ] ai-service — OpenAI anomaly detection, graceful degradation
- [ ] HashiCorp Vault — secrets management for all services
- [ ] Datadog APM — distributed tracing, custom metrics, dashboard
- [ ] Terraform — AWS VPC, RDS, S3 state backend
- [ ] Helm + Kubernetes — deployment charts, liveness/readiness probes
