# BeeFood Platform – AI Coding Agent Instructions

## 1. Project Overview

**Business Domain:** Cloud-native food ordering and delivery platform  
**Architecture Style:** Microservices + Event-Driven Architecture (EDA)  
**Scale Target:** Thousands of concurrent users and orders  
**Key Principles:** Service isolation, loose coupling, independent scalability, fault tolerance

### System Context
BeeFood is a production-grade microservices system inspired by real-world food delivery platforms. Each microservice is:
- **Independently deployable**
- **Owns its own database**
- **Communicates via REST (synchronous) or Kafka (asynchronous)**
- **Accessed only through Spring Cloud Gateway**

---

## 2. System Architecture Rules

### 2.1 Database-per-Service Pattern (CRITICAL)
- **RULE:** Each microservice MUST own and exclusively access its own database
- **FORBIDDEN:** Direct database queries across service boundaries
- **FORBIDDEN:** Shared databases or shared schemas between services
- **REQUIRED:** Use REST APIs or Kafka events to access other services' data

### 2.2 Communication Patterns
- **Synchronous:** REST APIs via Spring Cloud Gateway (for read-heavy, low-latency operations)
- **Asynchronous:** Kafka events (for write-heavy, eventual consistency scenarios)
- **FORBIDDEN:** Direct service-to-service HTTP calls bypassing the Gateway

### 2.3 API Gateway as Single Entry Point
- **RULE:** All external client requests MUST go through Spring Cloud Gateway
- **Gateway Responsibilities:**
  - Request routing
  - JWT authentication & authorization
  - Rate limiting
  - Load balancing
- **FORBIDDEN:** Exposing microservice ports directly to clients

### 2.4 Service Independence
- **RULE:** Services must be deployable independently
- **RULE:** A service failure must not cascade to other services
- **REQUIRED:** Implement fallback mechanisms (circuit breakers, default responses)


---

## 3. Microservices Responsibilities

### 3.1 Auth Service
**Purpose:** User authentication and authorization  
**Database:** PostgreSQL  
**Allowed Actions:**
- Register new users (delegates to Users Service for profile creation)
- Login users and issue JWT tokens
- Validate JWT tokens
- Refresh tokens
- Password reset flows

**Forbidden Actions:**
- ❌ Storing full user profiles (only auth credentials)
- ❌ Accessing other services' databases
- ❌ Business logic beyond authentication

---

### 3.2 Users Service
**Purpose:** Manage user profiles and roles  
**Database:** PostgreSQL  

**Allowed Actions:**
- CRUD operations on user profiles (customers, restaurant owners, drivers)
- Manage user roles and permissions
- Profile updates and preferences
- Query user information for other services

**Forbidden Actions:**
- ❌ Handling authentication (Auth Service responsibility)
- ❌ Storing order or restaurant data
- ❌ Direct Kafka event publishing (unless for user-related domain events)

---

### 3.3 Products Service
**Purpose:** Manage food & beverage inventory  
**Database:** PostgreSQL  
**Cache:** Redis (for popular products, menus)

**Allowed Actions:**
- CRUD operations on products and categories
- Inventory management
- Product search and filtering
- Cache frequently accessed products
- Consume `ORDER_CREATED` events to update inventory

**Forbidden Actions:**
- ❌ Managing restaurant information (Restaurants Service)
- ❌ Processing orders (Orders Service)
- ❌ Storing cache forever (use TTL)

**Caching Rules:**
- Cache popular products with 10-minute TTL
- Cache menu lists with 5-minute TTL
- Invalidate cache on product updates
- Use cache-aside pattern

---

### 3.4 Restaurants Service
**Purpose:** Manage restaurant profiles and menus  
**Database:** MongoDB (flexible schema for menu variations)

**Allowed Actions:**
- CRUD operations on restaurant profiles
- Manage menus and operating hours
- Restaurant search and filtering
- Update restaurant status (open/closed)

**Forbidden Actions:**
- ❌ Managing products directly (coordinate with Products Service)
- ❌ Processing orders (Orders Service)
- ❌ Managing user accounts (Users Service)

---

### 3.5 Orders Service (CORE SERVICE)
**Purpose:** Order creation, payment processing, order lifecycle management  
**Database:** PostgreSQL  
**Kafka Role:** Producer

**Allowed Actions:**
- Create new orders
- Process payments (integrate with payment gateway)
- Update order status
- Query order history
- **Publish Kafka events:** `ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`

**Forbidden Actions:**
- ❌ Assigning delivery drivers (Delivery Service)
- ❌ Directly updating inventory (publish event instead)
- ❌ Managing user or restaurant data

**Order State Machine:**
```
PENDING → CONFIRMED → PREPARING → READY → PICKED_UP → DELIVERED
         ↓
      CANCELLED (only before PREPARING)
```

---

### 3.6 Delivery Service
**Purpose:** Delivery logistics and driver management  
**Database:** MongoDB  
**Kafka Role:** Consumer

**Allowed Actions:**
- Consume `ORDER_CONFIRMED` events
- Assign delivery drivers
- Track delivery status in real-time
- Update delivery location (for live tracking)

**Forbidden Actions:**
- ❌ Modifying order details (Orders Service)
- ❌ Managing user profiles (Users Service)
- ❌ Synchronous API calls to Orders Service during event processing

---

### 3.7 Notification Service
**Purpose:** Send notifications (email, SMS, push)  
**Kafka Role:** Consumer  

**Allowed Actions:**
- Consume events from all services
- Send email/SMS/push notifications
- Log notification history

**Forbidden Actions:**
- ❌ Any business logic beyond notifications
- ❌ Database access to other services


---

## 4. Communication Patterns

### 4.1 When to Use REST (Synchronous)
✅ **Use REST when:**
- Client needs immediate response
- Read-heavy operations (GET requests)
- User-facing queries
- Low latency is critical

### 4.2 When to Use Kafka (Asynchronous)
✅ **Use Kafka when:**
- Write-heavy operations (state changes)
- Event-driven workflows
- Decoupling services
- Eventual consistency is acceptable
- Fan-out notifications (one event, multiple consumers)

### 4.3 Event Naming Conventions
**Format:** `<DOMAIN>_<ACTION>_<PAST_TENSE>`

**Examples:**
- ✅ `ORDER_CREATED`, `ORDER_CONFIRMED`, `DELIVERY_ASSIGNED`, `PAYMENT_COMPLETED`
- ❌ `create_order`, `ORDER_CREATE` (wrong format)

### 4.4 Kafka Event Schema (Required Fields)
All events must include:
- `eventId` (UUID) – Unique event identifier
- `eventType` (String) – Event name
- `timestamp` (ISO8601) – Event creation time
- `version` (String) – Schema version
- `payload` (Object) – Domain-specific data

### 4.5 Idempotency Rules
**CRITICAL:** All Kafka consumers MUST be idempotent
- Check if event already processed before executing logic
- Store `eventId` in database to detect duplicates
- Use unique constraints on business keys (e.g., `orderId`)

---

## 5. Database & Persistence Guidelines

### 5.1 PostgreSQL Services
**Services:** Auth, Users, Products, Orders  
**Rules:**
- Use JPA/Hibernate with Spring Data JPA
- Define clear entity relationships
- Use database migrations (Flyway or Liquibase)
- Never use `CascadeType.ALL` without careful consideration

### 5.2 MongoDB Services
**Services:** Restaurants, Delivery  
**Rules:**
- Use Spring Data MongoDB
- Design documents for query patterns (denormalization is OK)
- Index frequently queried fields
- Keep document size under 16MB

### 5.3 Transaction Boundaries
**RULE:** Transactions are local to a single service/database
- **FORBIDDEN:** Distributed transactions across services
- **REQUIRED:** Use Saga pattern for multi-service operations

### 5.4 Saga Pattern for Distributed Transactions
**Use Case:** Operations requiring coordination across multiple services

**Implementation Strategy:**
1. Service A performs local transaction and publishes event
2. Service B consumes event, performs local transaction
3. On failure, Service B publishes compensation event
4. Service A consumes compensation event and rolls back

**Example Flow:**
```
Orders Service: Create order (PENDING) → Publish ORDER_CREATED
Products Service: Reduce inventory → Publish INVENTORY_REDUCED (or INVENTORY_FAILED)
Orders Service: Listen to INVENTORY_FAILED → Mark order as CANCELLED
```

---

## 6. Security & Authentication

### 6.1 JWT Authentication Flow
1. User logs in via Auth Service
2. Auth Service issues JWT token (access + refresh)
3. Client includes JWT in `Authorization: Bearer <token>` header
4. Gateway validates JWT on every request
5. Gateway forwards request with `X-User-Id` header to services

### 6.2 Gateway Responsibility
- Validate JWT tokens
- Extract user information (userId, roles)
- Forward requests with `X-User-Id` and `X-User-Roles` headers
- Return 401 for invalid/expired tokens

### 6.3 Service-to-Service Authentication
**RULE:** Internal services trust `X-User-Id` header from Gateway
- **FORBIDDEN:** Re-validating JWT in microservices (Gateway already did this)
- **FORBIDDEN:** Services calling Auth Service on every request

### 6.4 Role-Based Access Control (RBAC)
**Roles:**
- `ROLE_CUSTOMER` – place orders, view own orders
- `ROLE_RESTAURANT_OWNER` – manage restaurants, menus
- `ROLE_DRIVER` – view assigned deliveries, update status
- `ROLE_ADMIN` – full access

**Enforcement:** Use `@PreAuthorize` annotations in controllers

---

## 7. Coding Standards

### 7.1 Technology Stack
- **Java Version:** 17+
- **Spring Boot:** 3.2+
- **Spring Cloud:** 2023.0.0+
- **Build Tool:** Maven
- **Code Style:** Google Java Style Guide

### 7.2 Package Structure
```
com.beefood.<service-name>
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data Transfer Objects
│   ├── request/
│   └── response/
├── entity/         # JPA entities / MongoDB documents
├── repository/     # Spring Data repositories
├── service/        # Business logic
│   └── impl/
├── exception/      # Custom exceptions
├── kafka/          # Kafka producers/consumers
│   ├── producer/
│   ├── consumer/
│   └── event/
├── mapper/         # Entity ↔ DTO mappers
└── util/           # Utility classes
```

### 7.3 Layered Architecture
**RULE:** Follow strict layering: Controller → Service → Repository

**Responsibilities:**
- **Controller:** HTTP handling, validation, DTO conversion
- **Service:** Business logic, transaction management
- **Repository:** Data access only

### 7.4 DTO vs Entity Separation
**RULE:** Never expose entities directly in APIs
- Prevents over-fetching / under-fetching
- Decouples API from database schema
- Avoids Jackson serialization issues (lazy loading, circular references)

### 7.5 Validation Rules
- Use Bean Validation (JSR-380) annotations: `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Valid`
- Validate at controller layer using `@Valid`
- Define custom validators for complex business rules

---

## 8. Error Handling & Logging

### 8.1 Global Exception Handling
**RULE:** Use `@ControllerAdvice` for centralized error handling
- Handle domain exceptions (e.g., `ResourceNotFoundException`)
- Handle validation exceptions (`MethodArgumentNotValidException`)
- Return standardized error responses

### 8.2 Error Response Format (Standardized)
All error responses must include:
- `code` (String) – Error code
- `message` (String) – Human-readable message
- `timestamp` (ISO8601) – When error occurred
- `errors` (Object, optional) – Field-level validation errors

### 8.3 Logging Rules
**✅ DO LOG:**
- Service startup/shutdown
- Incoming requests (at Gateway level)
- Business events (order created, payment processed)
- Kafka event publishing/consumption
- Error stack traces

**❌ DO NOT LOG:**
- Passwords or tokens
- Full credit card numbers
- Personally identifiable information (PII) without masking
- High-frequency events in tight loops

**Log Levels:**
- `TRACE` – Detailed debugging (rarely used)
- `DEBUG` – Variable values, flow (development only)
- `INFO` – Business events, milestones (production default)
- `WARN` – Recoverable errors, fallbacks
- `ERROR` – Unrecoverable errors

### 8.4 ELK Integration
- All services log to stdout/stderr in JSON format
- Logstash collects logs → Elasticsearch stores → Kibana visualizes
- Include `traceId` and `spanId` for distributed tracing

---

## 9. Kafka Usage Rules

### 9.1 Topic Naming Convention
**Format:** `<domain>.<action>`
**Examples:** `orders.created`, `orders.confirmed`, `payments.completed`, `deliveries.assigned`

### 9.2 Producer Responsibilities
- Configure `acks=all` for durability
- Set retry policy (e.g., retries=3)
- Publish events **after successful DB commit**
- Use JSON serialization for events

### 9.3 Consumer Responsibilities
- Use consumer groups for load balancing
- Disable auto-commit (use manual acknowledgment)
- Configure trusted packages for JSON deserialization
- Implement idempotency checks
- Handle exceptions gracefully (don't acknowledge on failure)

### 9.4 Retry & Dead Letter Queue (DLQ)
**RULE:** Configure retry policy and DLQ for failed messages
- Retry 3 times with exponential backoff
- After retries exhausted, send to DLQ topic: `<original-topic>.DLT`
- Monitor DLQ topics for manual intervention

---

## 10. Redis Caching Rules

### 10.1 What to Cache
**✅ Cache:**
- Product details (frequently viewed)
- Restaurant menus (read-heavy)
- Popular search results
- User sessions (optional)

**❌ Do NOT Cache:**
- Order details (changes frequently)
- Payment information (security risk)
- Real-time delivery locations

### 10.2 Cache TTL Strategy
- Products: 10 minutes
- Restaurant menus: 5 minutes
- Search results: 5 minutes
- Disable caching of null values

### 10.3 Cache-Aside Pattern
1. Check cache first
2. On cache miss, fetch from database
3. Update cache with TTL
4. Return result

### 10.4 Cache Invalidation Rules
**RULE:** Invalidate cache when data changes
- On UPDATE: Delete cache entry
- On DELETE: Delete cache entry
- Use `@CacheEvict` or manual `redisTemplate.delete()`

---

## 11. AI Coding Guidance

### 11.1 Always Respect Service Boundaries
**Before writing code, ask:**
1. Which service owns this data?
2. Am I accessing another service's database? (FORBIDDEN)
3. Should I use REST API or Kafka event?
4. Is this synchronous or asynchronous operation?

### 11.2 Follow Existing Patterns
**DO:**
- ✅ Analyze existing code in the service before creating new patterns
- ✅ Reuse existing DTOs, exceptions, utilities
- ✅ Follow the same package structure
- ✅ Use the same naming conventions

**DON'T:**
- ❌ Introduce new frameworks without justification
- ❌ Create duplicate utilities
- ❌ Mix coding styles

### 11.3 Avoid Over-Engineering
- Prefer standard Spring Boot features over custom solutions
- Use Spring Data JPA queries before writing native SQL
- Use `@Transactional` before implementing manual transaction management

### 11.4 Ask Before Breaking Rules
**If you need to:**
- Access another service's database → STOP and ask
- Introduce a new dependency → Ask for approval
- Change architectural patterns → Explain why

### 11.5 Write Tests
**RULE:** Every new feature must have tests

**Test Pyramid:**
- Unit tests (70%): Service layer logic
- Integration tests (20%): Repository + DB
- End-to-end tests (10%): API endpoints

### 11.6 Document Complex Logic
- Use JavaDoc for public APIs
- Document non-obvious design decisions
- Explain complex business rules


---

## 12. Prohibitions (CRITICAL)

### 12.1 ❌ NEVER Merge Services
- **FORBIDDEN:** Orders Service accessing Products database directly
- **CORRECT:** Use REST API or Kafka events for cross-service communication

### 12.2 ❌ NEVER Bypass Gateway
- **FORBIDDEN:** Client calling microservices directly
- **CORRECT:** All external requests must go through API Gateway with proper authentication

### 12.3 ❌ NEVER Share Databases
- **FORBIDDEN:** Multiple services connecting to the same database
- **FORBIDDEN:** Sharing schemas or tables between services
- **FORBIDDEN:** Direct SQL joins across service data

### 12.4 ❌ NEVER Create Long Synchronous Chains
- **FORBIDDEN:** Service A → Service B → Service C (high latency, tight coupling)
- **CORRECT:** Use Kafka for parallel, asynchronous processing

### 12.5 ❌ NEVER Store Sensitive Data in Logs or Cache
- **FORBIDDEN:** Logging passwords, tokens, credit card numbers, or PII
- **CORRECT:** Log only user IDs and mask sensitive information

### 12.6 ❌ NEVER Use `@Transactional` Across Services
- **FORBIDDEN:** Distributed transactions spanning multiple services/databases
- **CORRECT:** Use Saga pattern with Kafka for multi-service workflows

---

## 13. Quick Reference Checklist

Before committing code, verify:

- [ ] No cross-service database access
- [ ] All external requests go through Gateway
- [ ] DTOs used instead of entities in APIs
- [ ] Services communicate via REST or Kafka (not both for same use case)
- [ ] Kafka consumers are idempotent
- [ ] Transactions are local to one service
- [ ] Cache TTL is configured
- [ ] Errors are handled with `@ControllerAdvice`
- [ ] Sensitive data is not logged or cached
- [ ] Tests are written (unit + integration)
- [ ] Code follows existing patterns and structure
- [ ] Documentation is updated (if public API changed)

---

## 14. Additional Resources

### Project Documentation
- System architecture: `/README.md`
- Service-specific READMs: `/<service-name>/README.md`

### Spring Boot References
- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Kafka: https://spring.io/projects/spring-kafka

### Best Practices
- Microservices Patterns: https://microservices.io/patterns/
- 12-Factor App: https://12factor.net/
- Domain-Driven Design: https://martinfowler.com/bliki/DomainDrivenDesign.html

---

## 15. Final Notes for AI Agents

**Your mission:** Help developers build and maintain BeeFood without breaking its architecture.

**Golden Rules:**
1. **Service boundaries are sacred** – never cross them
2. **Gateway is the gatekeeper** – all traffic goes through it
3. **Events over calls** – prefer async Kafka over sync REST when possible
4. **Cache wisely** – cache reads, invalidate on writes
5. **Test everything** – no code without tests
6. **Ask when unsure** – better to ask than to break production

**When in doubt:**
- Read existing code in the service
- Follow established patterns
- Check this document
- Ask the developer

**Success criteria:**
- Code works on first attempt
- No architectural violations
- Follows team conventions
- Includes tests
- Well-documented

---

**Version:** 1.0  
**Last Updated:** 2025-12-26  
**Maintained by:** BeeFood Development Team
