# PRP: Kafka-Based Order Processing with Async Payment and Scheduled Expiration

## Context and Overview

This PRP defines the implementation of a Kafka-based event-driven order processing system with asynchronous payment simulation and scheduled order expiration. This is the MOST COMPLEX feature yet because it introduces:
1. **First message broker integration** (Kafka with Docker Compose)
2. **Event-driven architecture** (publish/subscribe pattern)
3. **Asynchronous processing** (payment simulation with delays)
4. **Scheduled jobs** (recurring background tasks)
5. **Random business logic** (50% payment success rate)
6. **Time-based operations** (order expiration after 10 minutes)

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database (already configured and running)
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction
- JWT authentication already implemented
- Orders module already implemented with OrderStatus enum (PENDING, PROCESSING, COMPLETED, EXPIRED)

**Feature Requirements:**
1. Setup Kafka in docker-compose for local development
2. When order is created, publish OrderCreated event to Kafka
3. Kafka consumer listens for OrderCreated events
4. Consumer updates order status from PENDING to PROCESSING
5. Consumer simulates payment processing with 5-second delay
6. For 50% of orders: update status to COMPLETED and publish OrderCompletedEvent
7. For other 50%: leave status as PROCESSING (no event)
8. Add scheduled job running every 60 seconds
9. Job finds PROCESSING orders older than 10 minutes
10. Job updates those orders to EXPIRED and publishes OrderExpiredEvent

**Existing Codebase Context:**
- Order entity at `src/main/java/com/example/zadanie/entity/Order.java:21` with createdAt field (line 51)
- OrderStatus enum at `src/main/java/com/example/zadanie/entity/OrderStatus.java:3` already has all 4 statuses
- OrderService at `src/main/java/com/example/zadanie/service/OrderService.java:22` with createOrder() at line 45
- OrderRepository at `src/main/java/com/example/zadanie/repository/OrderRepository.java:11` with findByStatus() method
- Docker Compose at `docker-compose.yml:1` with PostgreSQL configured
- Application config at `src/main/resources/application.yaml:1`
- Integration test pattern at `src/test/java/com/example/zadanie/integration/OrderControllerIntegrationTest.java:34`
- Main application class at `src/main/java/com/example/zadanie/ZadanieApplication.java:6`

## Research Findings and Documentation

### Spring Kafka Integration (NEW - No Kafka in project yet)

**Critical Resources:**
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/html/) - Official Spring Kafka reference documentation
- [Spring Kafka - Getting Started](https://spring.io/projects/spring-kafka#learn) - Spring Kafka project overview
- [Kafka in Spring Boot](https://www.baeldung.com/spring-kafka) - Comprehensive Baeldung guide
- [Spring Kafka Configuration](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/listener-annotation.html) - @KafkaListener documentation
- [Kafka Producer and Consumer with Spring Boot](https://www.javaguides.net/2022/05/spring-boot-kafka-tutorial.html) - Complete tutorial

**Key Points:**
- Add `spring-kafka` dependency to pom.xml (Spring Boot auto-configures Kafka)
- Add `spring-kafka-test` for testing with `@EmbeddedKafka`
- Configure Kafka in application.yaml with `spring.kafka.bootstrap-servers`
- Use `KafkaTemplate<String, String>` for sending messages (injected by Spring Boot)
- Use `@KafkaListener` annotation on methods to consume messages
- Default serialization is StringSerializer/StringDeserializer (JSON as String)
- Use `@EnableKafka` on configuration class (or main application class)
- Topics are auto-created by default if `spring.kafka.admin.auto-create=true`
- Consumer group ID required: `spring.kafka.consumer.group-id`

**Message Serialization:**
- Spring Boot includes Jackson for JSON serialization
- Send events as JSON strings: `objectMapper.writeValueAsString(event)`
- Deserialize in consumer: `objectMapper.readValue(message, EventClass.class)`
- Alternative: Use JsonSerializer/JsonDeserializer (more complex configuration)
- For simplicity, use String serialization with manual JSON conversion

### Kafka with Docker Compose

**Critical Resources:**
- [Confluent Kafka Docker Images](https://hub.docker.com/r/confluentinc/cp-kafka) - Official Confluent Kafka images
- [Apache Kafka Docker Quickstart](https://kafka.apache.org/documentation/#quickstart_dockercompose) - Official Kafka Docker guide
- [Kafka Docker Examples](https://github.com/conduktor/kafka-stack-docker-compose) - Community Docker Compose examples
- [KRaft Mode (Zookeeper-less)](https://kafka.apache.org/documentation/#kraft) - Modern Kafka without Zookeeper

**Key Points:**
- Use `confluentinc/cp-kafka:7.6.0` image (latest stable)
- Modern approach: Use KRaft mode (no Zookeeper needed)
- KRaft mode requires `KAFKA_PROCESS_ROLES=broker,controller`
- Set `KAFKA_NODE_ID=1` for single-broker setup
- Use `PLAINTEXT` listener for local development (no SSL)
- Expose port 9092 for external access (from Spring Boot app)
- Internal listener on 29092 for container-to-container communication
- Add healthcheck using `kafka-broker-api-versions` command
- Wait for Kafka to be ready before starting Spring Boot app

**Docker Compose Configuration:**
```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: zadanie-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_LOG_DIRS: /var/lib/kafka/data
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    volumes:
      - kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  kafka_data:
```

### Spring Task Scheduling (NEW - No scheduled tasks in project yet)

**Critical Resources:**
- [Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html) - Official Spring scheduling docs
- [Spring @Scheduled Annotation](https://www.baeldung.com/spring-scheduled-tasks) - Baeldung guide to @Scheduled
- [Scheduling Tasks in Spring Boot](https://spring.io/guides/gs/scheduling-tasks) - Spring official guide
- [Cron Expressions in Spring](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html) - Cron syntax

**Key Points:**
- Add `@EnableScheduling` to main application class (ZadanieApplication.java)
- Use `@Scheduled` annotation on methods to run periodically
- `@Scheduled(fixedRate = 60000)` runs every 60 seconds (60000 ms)
- `fixedRate` starts next execution 60s after previous execution START
- `fixedDelay` starts next execution 60s after previous execution END
- Use `fixedRate` for this feature (simpler, consistent interval)
- Scheduled methods must be in `@Component` or `@Service` classes
- Scheduled methods should be void return type
- Scheduled methods run in separate threads from request threads
- Use `@Transactional` on scheduled methods if modifying database
- No authentication needed for scheduled jobs (internal process)

**Best Practices:**
- Create separate package for scheduled jobs: `com.example.zadanie.scheduler`
- One class per scheduled task with `@Component` annotation
- Use descriptive method names: `expireProcessingOrders()`
- Log execution start and end for debugging
- Handle exceptions inside scheduled methods (don't let them bubble up)
- Consider using `@SchedulerLock` if running multiple app instances (not needed here)

### Time-Based Queries with JPA

**Critical Resources:**
- [Querying by Dates with Spring Data JPA](https://www.baeldung.com/spring-data-jpa-query-by-date) - Date query methods
- [Java LocalDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDateTime.html) - Java 21 LocalDateTime API
- [Spring Data JPA Method Names](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html) - Derived query methods

**Key Points:**
- Order entity has `createdAt` field with type `LocalDateTime`
- Create repository method: `findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff)`
- Method name automatically generates query: `WHERE status = ? AND created_at < ?`
- Calculate cutoff time: `LocalDateTime.now().minusMinutes(10)`
- Alternative: `LocalDateTime.now().minus(10, ChronoUnit.MINUTES)`
- `minusMinutes()` is simpler and more readable
- Query returns List<Order> of orders to expire
- Process in batch: iterate and update status for each order

### Testing Kafka Applications

**Critical Resources:**
- [Testing Spring Kafka Applications](https://docs.spring.io/spring-kafka/reference/testing.html) - Official testing guide
- [@EmbeddedKafka Annotation](https://www.baeldung.com/spring-boot-kafka-testing) - Baeldung testing guide
- [Spring Kafka Test](https://github.com/spring-projects/spring-kafka/tree/main/spring-kafka-test) - Spring Kafka Test module
- [Testing Scheduled Tasks](https://www.baeldung.com/spring-scheduled-test) - Testing @Scheduled methods

**Key Points:**
- Use `@EmbeddedKafka` annotation on integration tests
- Embedded Kafka starts in-memory broker for testing
- Configure topics: `@EmbeddedKafka(topics = {"order-events"})`
- Specify partitions and brokers: `@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9093"})`
- Use different port from production (9093 vs 9092)
- Combine with `@SpringBootTest` for full integration tests
- Use `KafkaTemplate` to send test messages
- Use `@KafkaListener` in test class to verify messages received
- Use `CountDownLatch` or `Awaitility` to wait for async processing
- For scheduled tasks: manually call scheduled methods in tests (don't wait 60 seconds)

### Jackson JSON Serialization (Already Available)

**Critical Resources:**
- [Jackson with Spring Boot](https://www.baeldung.com/jackson) - Baeldung Jackson guide
- [ObjectMapper](https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml.jackson/databind/ObjectMapper.html) - Jackson ObjectMapper docs

**Key Points:**
- Jackson already included via `spring-boot-starter-web`
- `ObjectMapper` auto-configured by Spring Boot (inject with `@Autowired`)
- Serialize to JSON: `objectMapper.writeValueAsString(event)`
- Deserialize from JSON: `objectMapper.readValue(json, EventClass.class)`
- Handle IOException when serializing/deserializing
- Events should be simple POJOs with getters/setters
- Use Lombok `@Data` or `@Getter/@Setter` for event classes
- Jackson auto-discovers fields via getters or public fields

### Random Number Generation

**Critical Resources:**
- [Java Random](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Random.html) - Random class
- [Java SecureRandom](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/security/SecureRandom.html) - SecureRandom class
- [ThreadLocalRandom](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadLocalRandom.html) - Thread-safe random

**Key Points:**
- For 50% probability, use `random.nextBoolean()` (returns true ~50% of time)
- Alternative: `random.nextInt(100) < 50` (returns true if random number 0-99 is less than 50)
- Use `java.util.Random` for simple cases (not cryptographically secure)
- Use `SecureRandom` if security matters (not needed here)
- For multithreaded environments: `ThreadLocalRandom.current().nextBoolean()`
- Since Kafka listeners may be multithreaded, use `ThreadLocalRandom`
- Create once at class level: `private final Random random = new Random();`
- Or use ThreadLocalRandom directly: `ThreadLocalRandom.current().nextBoolean()`

## Implementation Blueprint

### High-Level Approach

```
1. Maven Dependencies
   ├── Add spring-kafka dependency
   └── Add spring-kafka-test dependency (test scope)

2. Docker Compose Setup
   ├── Add Kafka service with KRaft mode (no Zookeeper)
   ├── Configure listeners and ports
   ├── Add healthcheck
   └── Add kafka_data volume

3. Application Configuration
   ├── Add Kafka bootstrap-servers configuration
   ├── Add consumer group-id
   ├── Add producer and consumer properties
   └── Enable auto-topic creation

4. Event DTOs (New Package: com.example.zadanie.event)
   ├── Create OrderCreatedEvent
   ├── Create OrderCompletedEvent
   ├── Create OrderExpiredEvent
   └── All events include: orderId, userId, productId, quantity, total, timestamp, status

5. Kafka Configuration
   ├── Add @EnableKafka to ZadanieApplication
   └── No additional config class needed (Spring Boot auto-configures)

6. Kafka Producer Service
   ├── Create OrderEventProducer service
   ├── Inject KafkaTemplate<String, String>
   ├── Inject ObjectMapper for JSON serialization
   ├── Method: publishOrderCreated(Order order)
   ├── Method: publishOrderCompleted(Order order)
   ├── Method: publishOrderExpired(Order order)
   └── Each method converts event to JSON and sends to Kafka topic

7. Update OrderService
   ├── Inject OrderEventProducer
   ├── In createOrder() method: after saving order, publish OrderCreatedEvent
   └── Keep existing logic intact (status still PENDING initially)

8. Kafka Consumer Service (Payment Processor)
   ├── Create OrderEventConsumer service with @Service
   ├── Add @KafkaListener on processOrderCreated() method
   ├── Listen to "order-events" topic
   ├── Parse OrderCreatedEvent from JSON message
   ├── Update order status from PENDING to PROCESSING
   ├── Save order with status PROCESSING
   ├── Publish OrderCreatedEvent again (now with PROCESSING status)
   ├── Sleep for 5 seconds (Thread.sleep(5000))
   ├── Use ThreadLocalRandom.current().nextBoolean() for 50% logic
   ├── If true: update to COMPLETED, publish OrderCompletedEvent
   ├── If false: leave as PROCESSING (no event)
   └── Wrap in try-catch for error handling

9. Scheduled Expiration Job
   ├── Create OrderExpirationScheduler in new package: scheduler
   ├── Add @Component annotation
   ├── Add @Scheduled(fixedRate = 60000) to expireOldOrders() method
   ├── Use @Transactional annotation
   ├── Calculate cutoff: LocalDateTime.now().minusMinutes(10)
   ├── Call orderRepository.findByStatusAndCreatedAtBefore(PROCESSING, cutoff)
   ├── For each order: update status to EXPIRED
   ├── For each order: publish OrderExpiredEvent
   └── Log expiration count

10. Repository Enhancement
    ├── Add findByStatusAndCreatedAtBefore() method to OrderRepository
    └── Spring Data JPA auto-generates query

11. Enable Scheduling
    ├── Add @EnableScheduling to ZadanieApplication.java
    └── This enables @Scheduled annotations

12. Testing
    ├── Create OrderEventProducerTest (unit test with Mockito)
    ├── Create OrderEventConsumerTest (integration test with @EmbeddedKafka)
    ├── Create OrderExpirationSchedulerTest (unit test - manually call scheduled method)
    └── Update OrderServiceTest to verify event publishing

13. Integration Testing
    ├── Start docker compose (Kafka + PostgreSQL)
    ├── Run Spring Boot application
    ├── Create order via REST API
    ├── Verify OrderCreatedEvent published to Kafka
    ├── Verify order status changes to PROCESSING
    ├── Wait 5 seconds and verify order status (50% COMPLETED, 50% PROCESSING)
    ├── Wait 10+ minutes and verify PROCESSING orders become EXPIRED
    └── Check Kafka topic for all events
```

### Package Structure

```
com.example.zadanie/
├── entity/
│   ├── Order.java (EXISTING - no changes)
│   ├── OrderStatus.java (EXISTING - no changes)
│   ├── Product.java (EXISTING - no changes)
│   └── User.java (EXISTING - no changes)
├── event/ (NEW PACKAGE)
│   ├── OrderCreatedEvent.java (NEW)
│   ├── OrderCompletedEvent.java (NEW)
│   └── OrderExpiredEvent.java (NEW)
├── repository/
│   ├── OrderRepository.java (UPDATE - add findByStatusAndCreatedAtBefore)
│   ├── ProductRepository.java (EXISTING - no changes)
│   └── UserRepository.java (EXISTING - no changes)
├── service/
│   ├── OrderService.java (UPDATE - inject OrderEventProducer, publish event in createOrder)
│   ├── OrderEventProducer.java (NEW)
│   ├── OrderEventConsumer.java (NEW)
│   └── Other services... (EXISTING - no changes)
├── scheduler/ (NEW PACKAGE)
│   └── OrderExpirationScheduler.java (NEW)
├── config/
│   └── SecurityConfig.java (EXISTING - no changes)
└── ZadanieApplication.java (UPDATE - add @EnableScheduling and @EnableKafka)
```

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Add Maven Dependencies

Update `pom.xml` by adding Spring Kafka dependencies in the `<dependencies>` section (after existing dependencies around line 112):

```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<!-- Spring Kafka Test -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Key Points:**
- Spring Boot manages version (no version tag needed)
- spring-kafka includes producer and consumer
- spring-kafka-test includes @EmbeddedKafka

**Validation:**
```bash
./mvnw dependency:tree | grep kafka
# Should show spring-kafka and spring-kafka-test
```

### Task 2: Update Docker Compose with Kafka

Update `docker-compose.yml` by adding Kafka service after the postgres service:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: zadanie-postgres
    environment:
      POSTGRES_DB: zadanie_db
      POSTGRES_USER: zadanie_user
      POSTGRES_PASSWORD: zadanie_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U zadanie_user -d zadanie_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: zadanie-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_LOG_DIRS: /var/lib/kafka/data
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    volumes:
      - kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  kafka_data:
```

**Key Points:**
- KRaft mode (no Zookeeper needed)
- Port 9092 for external connections (Spring Boot)
- Port 9093 for controller communication
- Replication factor 1 (single broker)
- Cluster ID is fixed string (required for KRaft)
- Healthcheck ensures Kafka is ready

**Validation:**
```bash
docker compose up -d kafka
docker compose ps
# Should show kafka as healthy

# Test Kafka connection
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
# Should return empty list (no topics yet) or show existing topics
```

### Task 3: Configure Kafka in Application

Update `src/main/resources/application.yaml` by adding Kafka configuration after the JWT section (around line 37):

```yaml
# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: zadanie-order-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

**Key Points:**
- `bootstrap-servers` points to Kafka broker
- `group-id` identifies consumer group (all consumers in group share partition load)
- `auto-offset-reset: earliest` starts from beginning if no offset stored
- String serializers/deserializers (we'll serialize events as JSON strings)
- Producer config separate from consumer config

### Task 4: Create Event DTOs

Create new package `src/main/java/com/example/zadanie/event/` and create three event classes:

**OrderCreatedEvent.java:**

```java
package com.example.zadanie.event;

import com.example.zadanie.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal total;
    private OrderStatus status;
    private LocalDateTime timestamp;
}
```

**OrderCompletedEvent.java:**

```java
package com.example.zadanie.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal total;
    private LocalDateTime timestamp;
}
```

**OrderExpiredEvent.java:**

```java
package com.example.zadanie.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderExpiredEvent {
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal total;
    private LocalDateTime timestamp;
}
```

**Key Points:**
- Simple POJOs with Lombok annotations
- Include all relevant order information
- timestamp is when event occurred (not order creation time)
- OrderCreatedEvent includes status (will be PENDING initially, then PROCESSING)
- Use BigDecimal for total (monetary value)
- Jackson auto-serializes via getters

### Task 5: Create Kafka Producer Service

Create `src/main/java/com/example/zadanie/service/OrderEventProducer.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.entity.Order;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderCreatedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "order-events";

    public void publishOrderCreated(Order order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotal(),
                order.getStatus(),
                LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, "order-created", json);
            log.info("Published OrderCreatedEvent for order ID: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCreatedEvent", e);
            throw new RuntimeException("Failed to publish order created event", e);
        }
    }

    public void publishOrderCompleted(Order order) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotal(),
                LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, "order-completed", json);
            log.info("Published OrderCompletedEvent for order ID: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCompletedEvent", e);
            throw new RuntimeException("Failed to publish order completed event", e);
        }
    }

    public void publishOrderExpired(Order order) {
        try {
            OrderExpiredEvent event = new OrderExpiredEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotal(),
                LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, "order-expired", json);
            log.info("Published OrderExpiredEvent for order ID: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderExpiredEvent", e);
            throw new RuntimeException("Failed to publish order expired event", e);
        }
    }
}
```

**Key Points:**
- `@Slf4j` for logging (Lombok annotation)
- Inject `KafkaTemplate<String, String>` (auto-configured by Spring Boot)
- Inject `ObjectMapper` for JSON serialization
- Single topic: "order-events" with different keys for event types
- Key is event type ("order-created", "order-completed", "order-expired")
- Convert event to JSON string
- Log successful publishing
- Wrap JsonProcessingException and rethrow as RuntimeException
- Throw exception to trigger transaction rollback if needed

### Task 6: Update OrderService to Publish Events

Update `src/main/java/com/example/zadanie/service/OrderService.java`:

Add field injection for OrderEventProducer:
```java
private final OrderEventProducer orderEventProducer;
```

Update the createOrder() method to publish event after saving order (around line 79, after `return orderRepository.save(order);`):

```java
@Transactional
public Order createOrder(OrderCreateRequest request) {
    // 1. Validate user exists
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

    // 2. Validate product exists
    Product product = productRepository.findById(request.getProductId())
        .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + request.getProductId()));

    // 3. Validate stock availability
    if (product.getStock() <= 0) {
        throw new IllegalArgumentException("Product is out of stock");
    }
    if (product.getStock() < request.getQuantity()) {
        throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStock() + ", Requested: " + request.getQuantity());
    }

    // 4. Calculate total price
    BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

    // 5. Create order
    Order order = new Order();
    order.setUser(user);
    order.setProduct(product);
    order.setQuantity(request.getQuantity());
    order.setTotal(total);
    order.setStatus(OrderStatus.PENDING);
    // createdAt and updatedAt auto-populated by Hibernate

    // 6. Decrement product stock
    product.setStock(product.getStock() - request.getQuantity());
    productRepository.save(product);

    // 7. Save order
    Order savedOrder = orderRepository.save(order);

    // 8. Publish OrderCreatedEvent to Kafka
    orderEventProducer.publishOrderCreated(savedOrder);

    return savedOrder;
}
```

**Key Points:**
- Add OrderEventProducer to constructor parameters (Lombok @RequiredArgsConstructor handles injection)
- Call `publishOrderCreated()` after order is saved (so order has ID)
- Event publishing happens within same transaction
- If event publishing fails, transaction rolls back (order not saved)
- Status is still PENDING when event published (consumer will update to PROCESSING)

### Task 7: Create Kafka Consumer Service (Payment Processor)

Create `src/main/java/com/example/zadanie/service/OrderEventConsumer.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.event.OrderCreatedEvent;
import com.example.zadanie.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "zadanie-order-group")
    @Transactional
    public void processOrderCreated(String message) {
        try {
            log.info("Received message from Kafka: {}", message);

            // Parse event
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            // Only process if status is PENDING (ignore PROCESSING events we republish)
            if (event.getStatus() != OrderStatus.PENDING) {
                log.info("Ignoring event for order {} with status {}", event.getOrderId(), event.getStatus());
                return;
            }

            log.info("Processing OrderCreatedEvent for order ID: {}", event.getOrderId());

            // 1. Find order
            Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

            // 2. Update status to PROCESSING
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
            log.info("Updated order {} status to PROCESSING", order.getId());

            // 3. Simulate payment processing delay (5 seconds)
            log.info("Simulating payment processing for order {}...", order.getId());
            Thread.sleep(5000);

            // 4. Random payment success (50% probability)
            boolean paymentSuccessful = ThreadLocalRandom.current().nextBoolean();

            if (paymentSuccessful) {
                // Payment successful - update to COMPLETED
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                log.info("Payment successful for order {}. Status updated to COMPLETED", order.getId());

                // Publish OrderCompletedEvent
                orderEventProducer.publishOrderCompleted(order);
            } else {
                // Payment failed - leave as PROCESSING (will expire via scheduled job)
                log.info("Payment failed for order {}. Status remains PROCESSING", order.getId());
            }

        } catch (InterruptedException e) {
            log.error("Payment processing interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent", e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }
}
```

**Key Points:**
- `@KafkaListener` annotation makes method consume messages from topic
- `groupId` matches application.yaml consumer group
- `@Transactional` ensures database updates are atomic
- Parse JSON message to OrderCreatedEvent
- Check status is PENDING (avoid processing our own PROCESSING events)
- Update status to PROCESSING and save
- `Thread.sleep(5000)` simulates payment delay (5 seconds)
- `ThreadLocalRandom.current().nextBoolean()` gives 50% true/false
- If true: update to COMPLETED and publish OrderCompletedEvent
- If false: leave as PROCESSING (no event, will expire in 10 minutes)
- Handle InterruptedException by re-interrupting thread
- Log all steps for debugging
- Exceptions cause transaction rollback

### Task 8: Add Repository Method for Time-Based Query

Update `src/main/java/com/example/zadanie/repository/OrderRepository.java`:

Add method after existing methods (around line 25):

```java
// Time-based query for order expiration
List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoffTime);
```

Add import:
```java
import java.time.LocalDateTime;
```

**Key Points:**
- Spring Data JPA auto-generates query from method name
- Query: `WHERE status = ? AND created_at < ?`
- Returns orders with given status created before cutoff time
- Used by scheduler to find orders older than 10 minutes

### Task 9: Create Scheduled Expiration Job

Create new package `src/main/java/com/example/zadanie/scheduler/` and create `OrderExpirationScheduler.java`:

```java
package com.example.zadanie.scheduler;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.service.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void expireOldOrders() {
        log.info("Running order expiration job...");

        // Calculate cutoff time (10 minutes ago)
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(10);

        // Find PROCESSING orders older than 10 minutes
        List<Order> ordersToExpire = orderRepository.findByStatusAndCreatedAtBefore(
            OrderStatus.PROCESSING,
            cutoffTime
        );

        if (ordersToExpire.isEmpty()) {
            log.info("No orders to expire");
            return;
        }

        log.info("Found {} orders to expire", ordersToExpire.size());

        // Update each order to EXPIRED and publish event
        for (Order order : ordersToExpire) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // Publish OrderExpiredEvent
            orderEventProducer.publishOrderExpired(order);

            log.info("Expired order ID: {}", order.getId());
        }

        log.info("Order expiration job completed. Expired {} orders", ordersToExpire.size());
    }
}
```

**Key Points:**
- `@Component` makes class a Spring bean
- `@Scheduled(fixedRate = 60000)` runs method every 60 seconds (60000 ms)
- `fixedRate` means next execution starts 60s after previous execution START
- `@Transactional` ensures all updates are atomic
- `LocalDateTime.now().minusMinutes(10)` calculates cutoff time
- Query finds PROCESSING orders created before cutoff
- Iterate through orders, update status, publish event
- Log count for monitoring
- If no orders found, log and return early
- All updates happen in single transaction

### Task 10: Enable Kafka and Scheduling in Application

Update `src/main/java/com/example/zadanie/ZadanieApplication.java`:

```java
package com.example.zadanie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class ZadanieApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZadanieApplication.class, args);
    }

}
```

**Key Points:**
- `@EnableKafka` enables Kafka listeners (@KafkaListener)
- `@EnableScheduling` enables scheduled tasks (@Scheduled)
- Both annotations required on @SpringBootApplication class or @Configuration class
- Without these, @KafkaListener and @Scheduled are ignored

### Task 11: Create Unit Tests

**OrderEventProducerTest.java:**

Create `src/test/java/com/example/zadanie/service/OrderEventProducerTest.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventProducer orderEventProducer;

    private Order testOrder;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldPublishOrderCreatedEvent() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        orderEventProducer.publishOrderCreated(testOrder);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate).send(eq("order-events"), eq("order-created"), anyString());
    }

    @Test
    void shouldPublishOrderCompletedEvent() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        orderEventProducer.publishOrderCompleted(testOrder);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate).send(eq("order-events"), eq("order-completed"), anyString());
    }

    @Test
    void shouldPublishOrderExpiredEvent() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        orderEventProducer.publishOrderExpired(testOrder);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate).send(eq("order-events"), eq("order-expired"), anyString());
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        // When & Then
        assertThatThrownBy(() -> orderEventProducer.publishOrderCreated(testOrder))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to publish order created event");
    }
}
```

**OrderExpirationSchedulerTest.java:**

Create `src/test/java/com/example/zadanie/scheduler/OrderExpirationSchedulerTest.java`:

```java
package com.example.zadanie.scheduler;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.service.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpirationSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderExpirationScheduler orderExpirationScheduler;

    private Order testOrder;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testProduct = new Product();
        testProduct.setId(1L);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PROCESSING);
    }

    @Test
    void shouldExpireOldOrders() {
        // Given
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testOrder));

        // When
        orderExpirationScheduler.expireOldOrders();

        // Then
        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class));
        verify(orderRepository).save(testOrder);
        verify(orderEventProducer).publishOrderExpired(testOrder);
        assert testOrder.getStatus() == OrderStatus.EXPIRED;
    }

    @Test
    void shouldDoNothingWhenNoOrdersToExpire() {
        // Given
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        orderExpirationScheduler.expireOldOrders();

        // Then
        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class));
        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).publishOrderExpired(any());
    }

    @Test
    void shouldExpireMultipleOrders() {
        // Given
        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(testUser);
        order1.setProduct(testProduct);
        order1.setQuantity(1);
        order1.setTotal(new BigDecimal("99.99"));
        order1.setStatus(OrderStatus.PROCESSING);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(testUser);
        order2.setProduct(testProduct);
        order2.setQuantity(2);
        order2.setTotal(new BigDecimal("199.98"));
        order2.setStatus(OrderStatus.PROCESSING);

        List<Order> orders = Arrays.asList(order1, order2);
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class)))
            .thenReturn(orders);

        // When
        orderExpirationScheduler.expireOldOrders();

        // Then
        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class));
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderEventProducer, times(2)).publishOrderExpired(any(Order.class));
    }
}
```

**Key Points:**
- Use `@ExtendWith(MockitoExtension.class)` for Mockito support
- Mock KafkaTemplate, ObjectMapper, OrderRepository, OrderEventProducer
- Test happy path (successful publishing)
- Test error handling (serialization failure)
- Test scheduler with orders to expire
- Test scheduler with no orders
- Test scheduler with multiple orders
- Verify method calls with `verify()`
- Don't need @EmbeddedKafka for unit tests (just mock KafkaTemplate)

### Task 12: Create Integration Test with Embedded Kafka

Create `src/test/java/com/example/zadanie/integration/OrderKafkaIntegrationTest.java`:

```java
package com.example.zadanie.integration;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.event.OrderCreatedEvent;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
import com.example.zadanie.service.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(topics = {"order-events"}, partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"})
@DirtiesContext
class OrderKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> records;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Set up Kafka consumer for testing
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties("order-events");

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // Create test data
        testUser = new User(null, "Test User", "test@example.com", "password");
        testUser = userRepository.save(testUser);

        testProduct = new Product(null, "Test Product", "Description", new BigDecimal("99.99"), 10, null);
        testProduct = productRepository.save(testProduct);

        testOrder = new Order(null, testUser, testProduct, 2, new BigDecimal("199.98"), OrderStatus.PENDING, null, null);
        testOrder = orderRepository.save(testOrder);
    }

    @AfterEach
    void tearDown() {
        container.stop();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldPublishOrderCreatedEventToKafka() throws Exception {
        // When
        orderEventProducer.publishOrderCreated(testOrder);

        // Then
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("order-created");

        String json = record.value();
        OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);

        assertThat(event.getOrderId()).isEqualTo(testOrder.getId());
        assertThat(event.getUserId()).isEqualTo(testUser.getId());
        assertThat(event.getProductId()).isEqualTo(testProduct.getId());
        assertThat(event.getQuantity()).isEqualTo(2);
        assertThat(event.getTotal()).isEqualByComparingTo(new BigDecimal("199.98"));
        assertThat(event.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldPublishOrderCompletedEventToKafka() throws Exception {
        // Given
        testOrder.setStatus(OrderStatus.COMPLETED);
        testOrder = orderRepository.save(testOrder);

        // When
        orderEventProducer.publishOrderCompleted(testOrder);

        // Then
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("order-completed");

        String json = record.value();
        assertThat(json).contains("\"orderId\":" + testOrder.getId());
        assertThat(json).contains("\"userId\":" + testUser.getId());
    }

    @Test
    void shouldPublishOrderExpiredEventToKafka() throws Exception {
        // Given
        testOrder.setStatus(OrderStatus.EXPIRED);
        testOrder = orderRepository.save(testOrder);

        // When
        orderEventProducer.publishOrderExpired(testOrder);

        // Then
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("order-expired");

        String json = record.value();
        assertThat(json).contains("\"orderId\":" + testOrder.getId());
    }
}
```

**Key Points:**
- `@EmbeddedKafka` starts in-memory Kafka broker for testing
- Use different port (9093) to avoid conflict with real Kafka (9092)
- `@DirtiesContext` ensures clean context for each test
- Set up test consumer to verify messages published to Kafka
- Use `BlockingQueue` to collect received messages
- `records.poll(10, TimeUnit.SECONDS)` waits up to 10 seconds for message
- Verify message key and JSON content
- Clean up test data in @AfterEach
- This tests ONLY the producer (publishing to Kafka)
- Consumer testing is complex (need to wait 5 seconds for payment processing)

## Gotchas and Key Considerations

### 1. Kafka Topic Auto-Creation
**Issue:** Topics must exist before publishing messages.
**Solution:** Spring Kafka auto-creates topics by default. No manual creation needed.
**Verification:** Check application.yaml has default config (no explicit `auto-create=false`).

### 2. KRaft Cluster ID
**Issue:** KRaft mode requires a unique cluster ID.
**Solution:** Use fixed cluster ID in docker-compose: `CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk`
**Note:** Cluster ID must be base64-encoded 16-byte value. The provided ID is valid.

### 3. Kafka Listener Processing Order Events Twice
**Issue:** Consumer receives both PENDING and PROCESSING OrderCreatedEvents.
**Solution:** Check event status and ignore PROCESSING events:
```java
if (event.getStatus() != OrderStatus.PENDING) {
    return; // Ignore
}
```

### 4. Thread.sleep() in Kafka Listener
**Issue:** `Thread.sleep(5000)` blocks Kafka listener thread for 5 seconds.
**Impact:** Only one order processed every 5 seconds (single-threaded listener).
**Solution:** This is acceptable for demo. For production, use `@Async` or increase listener concurrency.
**Note:** Don't use `@Async` in this implementation (adds complexity).

### 5. Transaction Rollback on Event Publishing Failure
**Issue:** If event publishing fails, should order creation rollback?
**Current Behavior:** Yes, because `publishOrderCreated()` is called within `@Transactional` method.
**Alternative:** Catch exception in OrderService and decide whether to rollback.
**Recommendation:** Keep current behavior (rollback) - ensures consistency.

### 6. Scheduled Job Running During Tests
**Issue:** Scheduled job runs every 60 seconds, even during tests.
**Solution:** Tests don't wait 60 seconds - manually call `expireOldOrders()` method.
**Note:** In integration tests, scheduler won't interfere (tests complete quickly).

### 7. Time Zone Considerations
**Issue:** `LocalDateTime.now()` uses JVM default time zone.
**Solution:** For this feature, time zone doesn't matter (relative time: 10 minutes ago).
**Note:** If deploying to different time zones, consider using `Instant` or `ZonedDateTime`.

### 8. Random Payment Success Not Deterministic
**Issue:** Tests can't reliably verify 50% success rate.
**Solution:** Unit tests mock the randomness. Integration tests accept non-deterministic behavior.
**Note:** For deterministic tests, inject Random as dependency and mock it.

### 9. Kafka Consumer Group Persistence
**Issue:** Consumer group offset stored by Kafka. Restarting app resumes from last processed message.
**Behavior:** If app crashes during payment processing, message reprocessed on restart.
**Solution:** This is acceptable (idempotency would require checking order status before processing).
**Note:** For production, implement idempotency checks.

### 10. Order Status Already Includes All Values
**Issue:** OrderStatus enum already has PENDING, PROCESSING, COMPLETED, EXPIRED.
**Impact:** No schema changes needed. No Liquibase migration.
**Verification:** Check `src/main/java/com/example/zadanie/entity/OrderStatus.java:3`

### 11. Security: Scheduled Jobs Don't Need Authentication
**Issue:** Scheduled jobs run as background processes (not HTTP requests).
**Impact:** No JWT token needed. Spring Security doesn't intercept.
**Verification:** No changes to SecurityConfig needed.

### 12. Embedded Kafka Port Conflict
**Issue:** Embedded Kafka (9093) must not conflict with real Kafka (9092).
**Solution:** Use port 9093 in `@EmbeddedKafka` annotation.
**Verification:** Tests pass even with Kafka running in Docker.

### 13. Jackson Serialization of BigDecimal
**Issue:** BigDecimal serialized as number in JSON (not string).
**Example:** `{"total": 199.98}` not `{"total": "199.98"}`
**Impact:** No issue - Jackson handles deserialization correctly.
**Note:** For precise decimal handling, could configure Jackson to use strings.

### 14. Lombok @Slf4j Requires Dependency
**Issue:** `@Slf4j` annotation requires Lombok and SLF4J.
**Status:** Lombok already in project. SLF4J included with Spring Boot.
**Verification:** Existing services use `@Slf4j` successfully.

### 15. Spring Boot Auto-Configuration for Kafka
**Issue:** Spring Boot auto-configures KafkaTemplate and KafkaListener.
**Impact:** No need for KafkaConfig class (unlike older Spring Kafka versions).
**Verification:** Just add properties to application.yaml.

## Quality Checklist

- [ ] Spring Kafka dependencies added to pom.xml
- [ ] Kafka service added to docker-compose.yml with KRaft mode
- [ ] kafka_data volume added to docker-compose.yml
- [ ] Kafka configuration added to application.yaml
- [ ] OrderCreatedEvent DTO created with all fields
- [ ] OrderCompletedEvent DTO created with all fields
- [ ] OrderExpiredEvent DTO created with all fields
- [ ] OrderEventProducer service created with 3 publish methods
- [ ] OrderService updated to inject OrderEventProducer
- [ ] OrderService.createOrder() calls publishOrderCreated()
- [ ] OrderEventConsumer service created with @KafkaListener
- [ ] Consumer checks event status (only process PENDING)
- [ ] Consumer updates status to PROCESSING
- [ ] Consumer sleeps for 5 seconds
- [ ] Consumer uses ThreadLocalRandom for 50% logic
- [ ] Consumer updates to COMPLETED and publishes event (50% case)
- [ ] Consumer leaves as PROCESSING (50% case)
- [ ] OrderRepository.findByStatusAndCreatedAtBefore() method added
- [ ] OrderExpirationScheduler created in scheduler package
- [ ] Scheduler has @Scheduled(fixedRate = 60000) annotation
- [ ] Scheduler calculates cutoff with minusMinutes(10)
- [ ] Scheduler queries for old PROCESSING orders
- [ ] Scheduler updates orders to EXPIRED
- [ ] Scheduler publishes OrderExpiredEvent for each order
- [ ] ZadanieApplication has @EnableKafka annotation
- [ ] ZadanieApplication has @EnableScheduling annotation
- [ ] OrderEventProducerTest created with unit tests
- [ ] OrderExpirationSchedulerTest created with unit tests
- [ ] OrderKafkaIntegrationTest created with @EmbeddedKafka
- [ ] All validation gates pass (compile, test, package, run)
- [ ] Kafka container starts and is healthy
- [ ] Orders created via API trigger OrderCreatedEvent
- [ ] Consumer processes events and updates status to PROCESSING
- [ ] Payment processing simulates 5-second delay
- [ ] ~50% of orders become COMPLETED, ~50% stay PROCESSING
- [ ] Scheduled job runs every 60 seconds (check logs)
- [ ] PROCESSING orders older than 10 minutes become EXPIRED
- [ ] All events visible in Kafka topic

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Clean and compile the project
./mvnw clean compile

# Expected output: BUILD SUCCESS

# 2. Run all tests
./mvnw test

# Expected output: All tests pass
# Expected: Existing tests + OrderEventProducerTest (4 tests) + OrderExpirationSchedulerTest (3 tests) + OrderKafkaIntegrationTest (3 tests) = 10+ new tests

# 3. Package the application
./mvnw package

# Expected output: BUILD SUCCESS, JAR created in target/

# 4. Start Kafka in Docker
docker compose up -d kafka

# Wait for Kafka to be healthy
docker compose ps

# Expected: kafka container status "healthy"

# 5. Verify Kafka is accessible
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Expected: Empty list (no topics yet) or order-events topic if auto-created

# 6. Start PostgreSQL (if not already running)
docker compose up -d postgres

# Expected: postgres container running and healthy

# 7. Start Spring Boot application
./mvnw spring-boot:run

# Expected output: Application starts on port 8080
# Expected logs:
# - "Started ZadanieApplication"
# - "Running order expiration job..." (every 60 seconds)
# - No Kafka connection errors

# 8. In a new terminal, get JWT token for testing
export TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123"}' | jq -r '.token')

echo $TOKEN

# Expected: JWT token printed (long string starting with "eyJ")

# 9. Create a test order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1,"productId":1,"quantity":2}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 201 CREATED with order JSON
# Example: {"id":1,"user":{...},"product":{...},"quantity":2,"total":199.98,"status":"PENDING",...}

# 10. Check application logs for Kafka events
# You should see:
# - "Published OrderCreatedEvent for order ID: 1"
# - "Received message from Kafka: ..."
# - "Processing OrderCreatedEvent for order ID: 1"
# - "Updated order 1 status to PROCESSING"
# - "Simulating payment processing for order 1..."
# - (after 5 seconds) "Payment successful for order 1..." OR "Payment failed for order 1..."

# 11. Wait 6 seconds and check order status
sleep 6
curl -X GET http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $TOKEN"

# Expected: Status is either "COMPLETED" (50% chance) or "PROCESSING" (50% chance)
# If COMPLETED: "Published OrderCompletedEvent" in logs
# If PROCESSING: Will expire in 10 minutes

# 12. Verify Kafka topic exists and has messages
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Expected output: order-events topic exists

docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --max-messages 10 \
  --timeout-ms 5000

# Expected: JSON messages with OrderCreatedEvent and possibly OrderCompletedEvent
# Example: {"orderId":1,"userId":1,"productId":1,"quantity":2,"total":199.98,"status":"PENDING",...}

# 13. Test scheduled job (create PROCESSING order older than 10 minutes)
# This is manual testing - in real scenario, wait 10 minutes and check logs for:
# "Found N orders to expire"
# "Expired order ID: X"
# "Published OrderExpiredEvent for order ID: X"

# Alternative: Manually update an order's createdAt to 11 minutes ago in database
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c \
  "UPDATE orders SET created_at = NOW() - INTERVAL '11 minutes', status = 'PROCESSING' WHERE id = 1;"

# Wait for next scheduled run (up to 60 seconds) and check logs
# Expected: "Found 1 orders to expire" and "Expired order ID: 1"

# 14. Verify order was expired
curl -X GET http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $TOKEN"

# Expected: Status is "EXPIRED"

# 15. Check Kafka for OrderExpiredEvent
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --max-messages 20 \
  --timeout-ms 5000 | grep -i expired

# Expected: OrderExpiredEvent JSON visible

# 16. Stop application and containers
docker compose down

# Expected: All containers stopped and removed
```

## Confidence Score: 6/10

**Rationale:**

**Strong Foundation (+):**
- Existing Orders module fully implemented
- All OrderStatus values already defined
- Clear requirements with exact timing (5 seconds, 10 minutes, 60 seconds)
- Docker Compose pattern exists (PostgreSQL already configured)
- Integration test pattern established
- All necessary project dependencies available (Lombok, Jackson)

**Comprehensive PRP (+):**
- Complete code snippets for every file (7 new files)
- Step-by-step tasks with exact file locations
- 10+ tests planned (unit + integration)
- 16 executable validation gates with example commands
- Extensive documentation URLs (15+ links)
- 15 gotchas identified with solutions

**Research Quality (+):**
- Documentation URLs for Kafka, scheduling, testing
- Links to official docs for Spring Kafka, Apache Kafka
- Docker Compose KRaft examples
- Testing guides for @EmbeddedKafka

**Risk Factors (-):**

1. **First Message Broker Integration (-1.5)**
   - No existing Kafka patterns in codebase
   - New concepts: topics, producers, consumers, @KafkaListener
   - Docker Compose Kafka configuration is complex (KRaft mode)
   - Easy to misconfigure listeners or serializers
   - Mitigation: Complete Docker Compose config, detailed application.yaml

2. **Asynchronous Processing Complexity (-1.0)**
   - Kafka consumer runs in separate thread
   - Thread.sleep() blocks listener thread
   - Race conditions possible (order updated while consumer processing)
   - Status checking logic (PENDING vs PROCESSING) easy to miss
   - Mitigation: Detailed consumer code, gotcha #3 addresses duplicate processing

3. **First Scheduled Task (-0.5)**
   - No existing @Scheduled examples in codebase
   - Must remember @EnableScheduling annotation
   - Time-based query method naming easy to get wrong
   - Transaction handling in scheduled methods not obvious
   - Mitigation: Complete scheduler code, gotcha #6 addresses testing

4. **Event-Driven Architecture Debugging (-0.5)**
   - Harder to debug than synchronous code
   - Events may be lost if Kafka down
   - Logs scattered across producer, consumer, scheduler
   - Integration testing requires waiting for async processing
   - Mitigation: Extensive logging in all components, integration test with @EmbeddedKafka

5. **Random Logic Non-Deterministic (-0.3)**
   - ThreadLocalRandom makes 50% logic hard to test
   - Integration tests may pass/fail randomly
   - Can't reliably verify both code paths in single test
   - Mitigation: Unit tests mock the random logic, gotcha #8 explains

6. **Multiple Moving Parts (-0.4)**
   - 7 new files (3 events, 2 services, 1 scheduler, 1 repo method)
   - 2 updates (OrderService, ZadanieApplication)
   - Kafka in Docker + Spring Boot coordination
   - Timing dependencies (5s delay, 60s schedule, 10m expiration)
   - Easy to miss one piece
   - Mitigation: Quality checklist with 38 items, detailed tasks

7. **Testing Asynchronous Code (-0.5)**
   - @EmbeddedKafka configuration complex
   - Need BlockingQueue to collect messages
   - Timeouts and polling required
   - Consumer tests need to wait 5+ seconds
   - Scheduler tests need manual time manipulation
   - Mitigation: Complete integration test code provided

8. **Docker Compose Kafka Complexity (-0.3)**
   - KRaft mode requires specific environment variables
   - CLUSTER_ID must be valid base64
   - Listener configuration has internal and external addresses
   - Healthcheck command not obvious
   - Mitigation: Complete docker-compose config, gotcha #2 explains cluster ID

**Strengths:**
- Orders module exists as foundation
- All prerequisites met (Lombok, Jackson, PostgreSQL)
- PRP provides complete implementations (not just guidance)
- Validation gates are comprehensive and executable
- Gotchas section covers all major pitfalls
- Clear separation of concerns (producer, consumer, scheduler)

**Weaknesses:**
- First time introducing message broker (big paradigm shift)
- Asynchronous processing harder to reason about than REST APIs
- Multiple timing dependencies (5s, 60s, 10m) to coordinate
- Testing requires @EmbeddedKafka (more complex than @DataJpaTest)
- Debugging event-driven systems requires log correlation

**Expected Outcome:**
An AI agent with access to this PRP, the codebase, and web search should be able to implement the Kafka order processing system, though with higher difficulty than previous modules due to:
- First message broker integration
- Asynchronous processing patterns
- Scheduled task introduction
- Event-driven architecture complexity
- Multiple timing constraints

The score of 6/10 reflects high complexity but comprehensive guidance. Implementation possible but requires careful attention to:
- Kafka configuration (Docker + Spring)
- Async processing logic (status checking)
- Scheduled job setup (@EnableScheduling)
- Testing with @EmbeddedKafka

**Recommendation for AI Agent:**
1. Implement tasks sequentially (don't skip ahead)
2. Validate Kafka in Docker before Spring Boot coding
3. Test each component independently (producer, consumer, scheduler)
4. Use extensive logging for debugging
5. Verify validation gates incrementally (don't wait until end)

## Documentation Sources

This PRP was created using research from the following sources:

**Spring Kafka:**
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/html/)
- [Spring Kafka - Getting Started](https://spring.io/projects/spring-kafka#learn)
- [Kafka in Spring Boot - Baeldung](https://www.baeldung.com/spring-kafka)
- [Spring Kafka Configuration](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/listener-annotation.html)
- [Kafka Producer and Consumer with Spring Boot](https://www.javaguides.net/2022/05/spring-boot-kafka-tutorial.html)

**Kafka Docker:**
- [Confluent Kafka Docker Images](https://hub.docker.com/r/confluentinc/cp-kafka)
- [Apache Kafka Docker Quickstart](https://kafka.apache.org/documentation/#quickstart_dockercompose)
- [Kafka Docker Examples](https://github.com/conduktor/kafka-stack-docker-compose)
- [KRaft Mode (Zookeeper-less)](https://kafka.apache.org/documentation/#kraft)

**Spring Scheduling:**
- [Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Spring @Scheduled Annotation - Baeldung](https://www.baeldung.com/spring-scheduled-tasks)
- [Scheduling Tasks in Spring Boot](https://spring.io/guides/gs/scheduling-tasks)
- [Cron Expressions in Spring](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html)

**JPA Time Queries:**
- [Querying by Dates with Spring Data JPA](https://www.baeldung.com/spring-data-jpa-query-by-date)
- [Java LocalDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDateTime.html)
- [Spring Data JPA Method Names](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)

**Testing:**
- [Testing Spring Kafka Applications](https://docs.spring.io/spring-kafka/reference/testing.html)
- [@EmbeddedKafka Annotation - Baeldung](https://www.baeldung.com/spring-boot-kafka-testing)
- [Spring Kafka Test](https://github.com/spring-projects/spring-kafka/tree/main/spring-kafka-test)
- [Testing Scheduled Tasks](https://www.baeldung.com/spring-scheduled-test)

**Jackson:**
- [Jackson with Spring Boot](https://www.baeldung.com/jackson)
- [ObjectMapper](https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/ObjectMapper.html)

**Random:**
- [Java Random](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Random.html)
- [ThreadLocalRandom](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadLocalRandom.html)
