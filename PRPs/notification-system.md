# PRP: Order Notification System with Email and Database Persistence

## Context and Overview

This PRP defines the implementation of a notification system that reacts to order events (OrderCompleted and OrderExpired) from Kafka, sending email notifications (fake/logged) and persisting notification records to the database. This feature builds upon the existing Kafka order processing system.

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database (already configured and running)
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction
- Kafka already configured and running (order-events topic exists)
- OrderEventProducer publishes OrderCompletedEvent and OrderExpiredEvent

**Feature Requirements:**
1. Create new `notifications` table with Liquibase migration
2. When OrderCompletedEvent is published:
   - Send fake email notification (log to console)
   - Save notification record to database
3. When OrderExpiredEvent is published:
   - Save notification record to database (no email)

**Existing Codebase Context:**
- Kafka already configured at `docker-compose.yml` with broker on port 9092
- OrderEventProducer at `src/main/java/com/example/zadanie/service/OrderEventProducer.java:19` publishes events
- OrderCompletedEvent at `src/main/java/com/example/zadanie/event/OrderCompletedEvent.java:13` contains orderId, userId, productId, quantity, total, timestamp
- OrderExpiredEvent at `src/main/java/com/example/zadanie/event/OrderExpiredEvent.java:13` has same structure
- OrderEventConsumer at `src/main/java/com/example/zadanie/service/OrderEventConsumer.java:19` shows @KafkaListener pattern
- Order entity at `src/main/java/com/example/zadanie/entity/Order.java:21` with user and product relationships
- User entity at `src/main/java/com/example/zadanie/entity/User.java:16` with id, name, email fields
- Liquibase master at `src/main/resources/db/changelog/db.changelog-master.yaml:1`
- Liquibase migration pattern at `src/main/resources/db/changelog/changes/v1.0/003-create-orders-table.yaml:1`
- Repository pattern at `src/main/java/com/example/zadanie/repository/OrderRepository.java:11`
- Service pattern at `src/main/java/com/example/zadanie/service/OrderEventProducer.java:16` with @RequiredArgsConstructor and @Slf4j
- Integration test pattern at `src/test/java/com/example/zadanie/integration/OrderKafkaIntegrationTest.java:39`

## Research Findings and Documentation

### Spring Kafka Consumer Patterns

**Critical Resources:**
- [Multiple Event Types in the Same Kafka Topic](https://www.confluent.io/blog/multiple-event-types-in-the-same-kafka-topic/) - Confluent guide on handling multiple events
- [Spring Kafka: Configure Multiple Listeners on Same Topic](https://www.baeldung.com/spring-kafka-multiple-listeners-same-topic) - Baeldung guide
- [Spring Boot Kafka Multiple Consumers Example](https://howtodoinjava.com/kafka/multiple-consumers-example/) - Multiple consumer patterns
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/html/) - Official documentation

**Key Points:**
- Multiple @KafkaListener methods can listen to the same topic
- Each listener can have a different consumer group ID
- Listener methods can receive `ConsumerRecord<String, String>` to access message key
- Message key can be used to filter event types ("order-completed", "order-expired")
- Same consumer group processes each message once
- Different consumer groups process same message independently
- For notifications, we'll create a separate listener focused on completed/expired events

### Email Notification Best Practices (2025)

**Critical Resources:**
- [Building Email Notification Systems in Spring Boot](https://dev.to/ayshriv/building-synchronous-email-notification-systems-in-spring-boot-a-step-by-step-guide-1lik) - DEV Community guide
- [How to Set Up Email Notifications in Spring Boot](https://medium.com/@AlexanderObregon/how-to-set-up-email-notifications-in-spring-boot-applications-b5a2574c5e8f) - Medium tutorial
- [Guide to Spring Email](https://www.baeldung.com/spring-email) - Baeldung comprehensive guide
- [Sending Emails in Spring Boot 2024](https://mailtrap.io/blog/spring-send-email/) - Mailtrap guide

**Key Points:**
- Spring Boot uses `JavaMailSender` interface for email sending
- For fake/mock email, just use SLF4J logger (no dependencies needed)
- Real email requires `spring-boot-starter-mail` dependency
- Use @Async for non-blocking email sending in production
- For this feature: log email content to console (fake sending)
- Email notification should include: recipient, subject, body
- Log format: "Sending email to {email}: {subject} - {body}"

### JPA Entity and Repository Patterns (Already in Project)

**Critical Resources:**
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/) - Official docs
- [JPA Entity Mapping](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html) - Jakarta Persistence spec

**Key Points:**
- Entity pattern: @Entity, @Table, @Id, @GeneratedValue(strategy = GenerationType.IDENTITY)
- Use Lombok @Data for getters/setters/toString
- Use @CreationTimestamp for automatic timestamp population
- Foreign keys with @ManyToOne and @JoinColumn
- Repository extends JpaRepository<Entity, ID>
- Spring Data JPA auto-generates query methods from method names

### Liquibase Database Migrations (Already in Project)

**Critical Resources:**
- [Liquibase YAML Format](https://docs.liquibase.com/concepts/changelogs/yaml-format.html) - Official YAML guide
- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html) - Best practices guide

**Key Points:**
- Migrations in `src/main/resources/db/changelog/changes/v1.0/`
- Format: `{number}-{description}.yaml` (e.g., `005-create-notifications-table.yaml`)
- Include in master changelog at `db.changelog-master.yaml`
- Changeset structure: id, author, changes, rollback
- Use `createTable` with columns and constraints
- Use `addForeignKeyConstraint` for relationships
- Provide rollback strategy (usually `dropTable`)

## Implementation Blueprint

### High-Level Approach

```
1. Database Schema (Liquibase Migration)
   ├── Create notifications table with columns:
   │   ├── id (BIGINT, auto-increment, primary key)
   │   ├── order_id (BIGINT, foreign key to orders)
   │   ├── user_id (BIGINT, foreign key to users)
   │   ├── notification_type (VARCHAR(30): ORDER_COMPLETED, ORDER_EXPIRED)
   │   ├── message (TEXT)
   │   ├── email_sent (BOOLEAN, default false)
   │   └── created_at (TIMESTAMP, default CURRENT_TIMESTAMP)
   ├── Add foreign key constraint to orders table
   ├── Add foreign key constraint to users table
   └── Add rollback strategy (dropTable)

2. Domain Model (Entity)
   ├── Create NotificationType enum (ORDER_COMPLETED, ORDER_EXPIRED)
   └── Create Notification entity with JPA annotations

3. Repository Layer
   └── Create NotificationRepository extending JpaRepository

4. Service Layer
   ├── Create NotificationService with methods:
   │   ├── createOrderCompletedNotification(OrderCompletedEvent)
   │   ├── createOrderExpiredNotification(OrderExpiredEvent)
   │   └── sendFakeEmail(Notification) - logs email details
   └── Inject OrderRepository, UserRepository for entity lookups

5. Kafka Consumer Layer
   ├── Create NotificationEventConsumer (separate from OrderEventConsumer)
   ├── Listen to "order-events" topic with different consumer group
   ├── Filter messages by key ("order-completed", "order-expired")
   ├── Deserialize events using ObjectMapper
   ├── Call NotificationService methods
   └── Use @Transactional for database operations

6. Testing
   ├── Create NotificationServiceTest (unit test with Mockito)
   ├── Create NotificationRepositoryTest (integration test with @DataJpaTest)
   ├── Create NotificationEventConsumerTest (integration test with @EmbeddedKafka)
   └── Update OrderKafkaIntegrationTest to verify notification creation
```

### Package Structure

```
com.example.zadanie/
├── entity/
│   ├── Notification.java (NEW)
│   ├── NotificationType.java (NEW - enum)
│   └── Order.java, User.java (EXISTING - no changes)
├── event/
│   ├── OrderCompletedEvent.java (EXISTING - no changes)
│   └── OrderExpiredEvent.java (EXISTING - no changes)
├── repository/
│   ├── NotificationRepository.java (NEW)
│   └── OrderRepository.java, UserRepository.java (EXISTING - no changes)
├── service/
│   ├── NotificationService.java (NEW)
│   ├── NotificationEventConsumer.java (NEW)
│   ├── OrderEventProducer.java (EXISTING - no changes)
│   └── OrderEventConsumer.java (EXISTING - no changes)
└── ZadanieApplication.java (EXISTING - no changes, @EnableKafka already there)
```

### Detailed Pseudocode

```java
// NotificationType Enum
enum NotificationType {
  ORDER_COMPLETED,
  ORDER_EXPIRED
}

// Notification Entity
@Entity
class Notification {
  @Id @GeneratedValue(strategy = IDENTITY)
  id: Long

  @ManyToOne
  @JoinColumn(name = "order_id", nullable = false)
  order: Order

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  user: User

  @Enumerated(EnumType.STRING)
  notificationType: NotificationType

  @Column(columnDefinition = "TEXT")
  message: String

  @Column(nullable = false)
  emailSent: Boolean = false

  @CreationTimestamp
  createdAt: LocalDateTime
}

// NotificationRepository
interface NotificationRepository extends JpaRepository<Notification, Long> {
  // Spring Data JPA auto-generates implementations
  List<Notification> findByUserId(Long userId)
  List<Notification> findByOrderId(Long orderId)
  List<Notification> findByNotificationType(NotificationType type)
}

// NotificationService
@Service
@RequiredArgsConstructor
@Slf4j
class NotificationService {

  private final NotificationRepository notificationRepository
  private final OrderRepository orderRepository
  private final UserRepository userRepository

  @Transactional
  method createOrderCompletedNotification(event: OrderCompletedEvent) {
    1. Find order by event.orderId (throw exception if not found)
    2. Find user by event.userId (throw exception if not found)
    3. Build message: "Your order #{orderId} has been completed successfully. Total: ${total}"
    4. Create notification entity:
       - order = found order
       - user = found user
       - notificationType = ORDER_COMPLETED
       - message = built message
       - emailSent = false (will be updated after sending)
    5. Save notification to database
    6. Call sendFakeEmail(notification, user.email)
    7. Update notification.emailSent = true
    8. Save notification again
    9. Return notification
  }

  @Transactional
  method createOrderExpiredNotification(event: OrderExpiredEvent) {
    1. Find order by event.orderId (throw exception if not found)
    2. Find user by event.userId (throw exception if not found)
    3. Build message: "Your order #{orderId} has expired due to payment timeout."
    4. Create notification entity:
       - order = found order
       - user = found user
       - notificationType = ORDER_EXPIRED
       - message = built message
       - emailSent = false (no email sent for expired orders)
    5. Save notification to database
    6. Return notification (no email sending)
  }

  method sendFakeEmail(notification: Notification, recipientEmail: String) {
    1. Log info: "========================================="
    2. Log info: "SENDING EMAIL NOTIFICATION"
    3. Log info: "To: {recipientEmail}"
    4. Log info: "Subject: Order Notification - Order #{notification.order.id}"
    5. Log info: "Body: {notification.message}"
    6. Log info: "========================================="
    7. // In real implementation, this would use JavaMailSender
  }
}

// NotificationEventConsumer
@Service
@RequiredArgsConstructor
@Slf4j
class NotificationEventConsumer {

  private final NotificationService notificationService
  private final ObjectMapper objectMapper

  @KafkaListener(
    topics = "order-events",
    groupId = "zadanie-notification-group",  // Different group from order processor
    containerFactory = "kafkaListenerContainerFactory"
  )
  @Transactional
  method consumeOrderEvents(record: ConsumerRecord<String, String>) {
    try {
      String key = record.key()
      String message = record.value()

      log.info("Notification consumer received event with key: {}", key)

      if (key == "order-completed") {
        // Deserialize to OrderCompletedEvent
        OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class)
        log.info("Processing OrderCompletedEvent for order ID: {}", event.orderId)

        // Create notification (includes email sending)
        notificationService.createOrderCompletedNotification(event)
        log.info("Created notification and sent email for completed order {}", event.orderId)
      }
      else if (key == "order-expired") {
        // Deserialize to OrderExpiredEvent
        OrderExpiredEvent event = objectMapper.readValue(message, OrderExpiredEvent.class)
        log.info("Processing OrderExpiredEvent for order ID: {}", event.orderId)

        // Create notification (no email)
        notificationService.createOrderExpiredNotification(event)
        log.info("Created notification for expired order {}", event.orderId)
      }
      else {
        // Ignore other event types (order-created)
        log.debug("Ignoring event with key: {}", key)
      }
    }
    catch (JsonProcessingException e) {
      log.error("Failed to deserialize event", e)
      throw new RuntimeException("Failed to process notification event", e)
    }
    catch (Exception e) {
      log.error("Failed to create notification", e)
      throw new RuntimeException("Failed to create notification", e)
    }
  }
}
```

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Create Liquibase Migration for Notifications Table

Create `src/main/resources/db/changelog/changes/v1.0/005-create-notifications-table.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: v1.0-005-create-notifications-table
      author: liquibase
      changes:
        - createTable:
            tableName: notifications
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: order_id
                  type: BIGINT
                  constraints:
                    nullable: false
              - column:
                  name: user_id
                  type: BIGINT
                  constraints:
                    nullable: false
              - column:
                  name: notification_type
                  type: VARCHAR(30)
                  constraints:
                    nullable: false
              - column:
                  name: message
                  type: TEXT
                  constraints:
                    nullable: false
              - column:
                  name: email_sent
                  type: BOOLEAN
                  defaultValueBoolean: false
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false

        - addForeignKeyConstraint:
            baseTableName: notifications
            baseColumnNames: order_id
            referencedTableName: orders
            referencedColumnNames: id
            constraintName: fk_notifications_order
            onDelete: CASCADE

        - addForeignKeyConstraint:
            baseTableName: notifications
            baseColumnNames: user_id
            referencedTableName: users
            referencedColumnNames: id
            constraintName: fk_notifications_user
            onDelete: CASCADE

      rollback:
        - dropTable:
            tableName: notifications
```

**Key Points:**
- Table name: `notifications` (lowercase, plural)
- id: auto-increment BIGINT primary key
- order_id and user_id: foreign keys with CASCADE delete
- notification_type: VARCHAR(30) to store enum values
- message: TEXT for longer notification messages
- email_sent: BOOLEAN with default false
- created_at: auto-populated timestamp
- Rollback: drop table completely

### Task 2: Update Liquibase Master Changelog

Update `src/main/resources/db/changelog/db.changelog-master.yaml` by adding the new migration at the end (after line 9):

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/v1.0/001-create-users-table.yaml
  - include:
      file: db/changelog/changes/v1.0/002-create-products-table.yaml
  - include:
      file: db/changelog/changes/v1.0/003-create-orders-table.yaml
  - include:
      file: db/changelog/changes/v1.0/004-seed-data.yaml
  - include:
      file: db/changelog/changes/v1.0/005-create-notifications-table.yaml
```

**Key Points:**
- Add as last include in the file
- Path is relative: `db/changelog/changes/v1.0/005-create-notifications-table.yaml`
- Liquibase will run this migration on next application start

**Validation:**
```bash
./mvnw clean compile
# Should compile successfully

docker compose up -d postgres
./mvnw liquibase:update
# Should run migration and create notifications table

# Verify table exists
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "\d notifications"
# Should show notifications table structure
```

### Task 3: Create NotificationType Enum

Create `src/main/java/com/example/zadanie/entity/NotificationType.java`:

```java
package com.example.zadanie.entity;

public enum NotificationType {
    ORDER_COMPLETED,
    ORDER_EXPIRED
}
```

**Key Points:**
- Simple enum with two values
- Stored as VARCHAR(30) in database
- Used with @Enumerated(EnumType.STRING) in entity
- Uppercase with underscore (Java enum convention)

### Task 4: Create Notification Entity

Create `src/main/java/com/example/zadanie/entity/Notification.java`:

```java
package com.example.zadanie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotNull(message = "Message is required")
    private String message;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

**Key Points:**
- Follows same pattern as Order entity
- @ManyToOne relationships with Order and User (LAZY fetch)
- @Enumerated(EnumType.STRING) stores enum as string in DB
- emailSent defaults to false
- @CreationTimestamp auto-populates createdAt
- Lombok @Data generates getters/setters/toString
- No updatedAt field (notifications are immutable after creation)

### Task 5: Create NotificationRepository

Create `src/main/java/com/example/zadanie/repository/NotificationRepository.java`:

```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.Notification;
import com.example.zadanie.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // JpaRepository provides:
    // - save(Notification notification)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)

    // Custom query methods
    List<Notification> findByUserId(Long userId);

    List<Notification> findByOrderId(Long orderId);

    List<Notification> findByNotificationType(NotificationType type);

    List<Notification> findByUserIdAndNotificationType(Long userId, NotificationType type);
}
```

**Key Points:**
- Extends JpaRepository<Notification, Long>
- Spring Data JPA auto-generates query implementations
- Methods follow naming convention for derived queries
- findByUserId: find all notifications for a user
- findByOrderId: find all notifications for an order
- findByNotificationType: find by notification type
- Combined method for user + type filtering

### Task 6: Create NotificationService

Create `src/main/java/com/example/zadanie/service/NotificationService.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.entity.Notification;
import com.example.zadanie.entity.NotificationType;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.User;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.example.zadanie.repository.NotificationRepository;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createOrderCompletedNotification(OrderCompletedEvent event) {
        log.info("Creating notification for completed order {}", event.getOrderId());

        // Find order and user
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

        User user = userRepository.findById(event.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + event.getUserId()));

        // Build notification message
        String message = String.format(
            "Your order #%d has been completed successfully. Total: $%.2f. Thank you for your purchase!",
            event.getOrderId(),
            event.getTotal()
        );

        // Create notification entity
        Notification notification = new Notification();
        notification.setOrder(order);
        notification.setUser(user);
        notification.setNotificationType(NotificationType.ORDER_COMPLETED);
        notification.setMessage(message);
        notification.setEmailSent(false);

        // Save notification
        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", notification.getId());

        // Send fake email
        sendFakeEmail(notification, user.getEmail());

        // Update email sent flag
        notification.setEmailSent(true);
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Transactional
    public Notification createOrderExpiredNotification(OrderExpiredEvent event) {
        log.info("Creating notification for expired order {}", event.getOrderId());

        // Find order and user
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

        User user = userRepository.findById(event.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + event.getUserId()));

        // Build notification message
        String message = String.format(
            "Your order #%d has expired due to payment timeout. Please create a new order if you still wish to purchase.",
            event.getOrderId()
        );

        // Create notification entity
        Notification notification = new Notification();
        notification.setOrder(order);
        notification.setUser(user);
        notification.setNotificationType(NotificationType.ORDER_EXPIRED);
        notification.setMessage(message);
        notification.setEmailSent(false); // No email sent for expired orders

        // Save notification
        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {} (no email sent for expired orders)", notification.getId());

        return notification;
    }

    private void sendFakeEmail(Notification notification, String recipientEmail) {
        log.info("=========================================");
        log.info("SENDING EMAIL NOTIFICATION");
        log.info("To: {}", recipientEmail);
        log.info("Subject: Order Notification - Order #{}", notification.getOrder().getId());
        log.info("Body: {}", notification.getMessage());
        log.info("=========================================");

        // In a real implementation, this would use JavaMailSender:
        // mailSender.send(email);
    }
}
```

**Key Points:**
- @Service makes it a Spring bean
- @RequiredArgsConstructor injects dependencies
- @Slf4j provides logger
- @Transactional ensures atomic database operations
- createOrderCompletedNotification: creates notification, sends email, updates emailSent flag
- createOrderExpiredNotification: creates notification, NO email
- sendFakeEmail: logs email details (fake implementation)
- Throws IllegalArgumentException if order or user not found
- Uses String.format for clean message formatting

### Task 7: Create NotificationEventConsumer

Create `src/main/java/com/example/zadanie/service/NotificationEventConsumer.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "order-events",
        groupId = "zadanie-notification-group"
    )
    @Transactional
    public void consumeOrderEvents(ConsumerRecord<String, String> record) {
        try {
            String key = record.key();
            String message = record.value();

            log.info("Notification consumer received event with key: {}", key);

            if ("order-completed".equals(key)) {
                // Deserialize to OrderCompletedEvent
                OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);
                log.info("Processing OrderCompletedEvent for order ID: {}", event.getOrderId());

                // Create notification (includes email sending)
                notificationService.createOrderCompletedNotification(event);
                log.info("Created notification and sent email for completed order {}", event.getOrderId());

            } else if ("order-expired".equals(key)) {
                // Deserialize to OrderExpiredEvent
                OrderExpiredEvent event = objectMapper.readValue(message, OrderExpiredEvent.class);
                log.info("Processing OrderExpiredEvent for order ID: {}", event.getOrderId());

                // Create notification (no email)
                notificationService.createOrderExpiredNotification(event);
                log.info("Created notification for expired order {}", event.getOrderId());

            } else {
                // Ignore other event types (order-created)
                log.debug("Ignoring event with key: {}", key);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event", e);
            throw new RuntimeException("Failed to process notification event", e);
        } catch (Exception e) {
            log.error("Failed to create notification", e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }
}
```

**Key Points:**
- Separate @KafkaListener from OrderEventConsumer
- Different consumer group: "zadanie-notification-group"
- Receives `ConsumerRecord<String, String>` to access message key
- Filters events by key: "order-completed" and "order-expired"
- Ignores "order-created" events (handled by OrderEventConsumer)
- Uses ObjectMapper to deserialize JSON to event objects
- @Transactional ensures database operations are atomic
- Throws RuntimeException on errors (triggers transaction rollback)
- Extensive logging for debugging

### Task 8: Create Unit Tests for NotificationService

Create `src/test/java/com/example/zadanie/service/NotificationServiceTest.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.entity.*;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.example.zadanie.repository.NotificationRepository;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderCompletedEvent completedEvent;
    private OrderExpiredEvent expiredEvent;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.COMPLETED);

        completedEvent = new OrderCompletedEvent(
            1L, 1L, 1L, 2, new BigDecimal("199.98"), LocalDateTime.now()
        );

        expiredEvent = new OrderExpiredEvent(
            1L, 1L, 1L, 2, new BigDecimal("199.98"), LocalDateTime.now()
        );
    }

    @Test
    void shouldCreateOrderCompletedNotification() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // When
        Notification notification = notificationService.createOrderCompletedNotification(completedEvent);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
        assertThat(notification.getEmailSent()).isTrue();
        assertThat(notification.getMessage()).contains("completed successfully");
        verify(notificationRepository, times(2)).save(any(Notification.class)); // Once before email, once after
        verify(orderRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldCreateOrderExpiredNotification() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // When
        Notification notification = notificationService.createOrderExpiredNotification(expiredEvent);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_EXPIRED);
        assertThat(notification.getEmailSent()).isFalse(); // No email for expired orders
        assertThat(notification.getMessage()).contains("expired");
        verify(notificationRepository, times(1)).save(any(Notification.class)); // Only once (no email)
        verify(orderRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createOrderCompletedNotification(completedEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createOrderCompletedNotification(completedEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }
}
```

**Key Points:**
- Use Mockito for mocking repositories
- Test both createOrderCompletedNotification and createOrderExpiredNotification
- Verify emailSent flag is true for completed, false for expired
- Verify save() called twice for completed (before and after email)
- Verify save() called once for expired (no email)
- Test error cases: order not found, user not found
- Use AssertJ for fluent assertions

### Task 9: Create Repository Integration Test

Create `src/test/java/com/example/zadanie/repository/NotificationRepositoryTest.java`:

```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Create test user
        testUser = new User(null, "Test User", "test@example.com", "password");
        testUser = userRepository.save(testUser);

        // Create test product
        Product testProduct = new Product(null, "Test Product", "Description", new BigDecimal("99.99"), 10, null);
        testProduct = productRepository.save(testProduct);

        // Create test order
        testOrder = new Order(null, testUser, testProduct, 2, new BigDecimal("199.98"), OrderStatus.COMPLETED, null, null);
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    void shouldSaveNotification() {
        // Given
        Notification notification = new Notification();
        notification.setOrder(testOrder);
        notification.setUser(testUser);
        notification.setNotificationType(NotificationType.ORDER_COMPLETED);
        notification.setMessage("Test notification");
        notification.setEmailSent(true);

        // When
        Notification saved = notificationRepository.save(notification);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrder().getId()).isEqualTo(testOrder.getId());
        assertThat(saved.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByUserId() {
        // Given
        Notification notification1 = new Notification(null, testOrder, testUser, NotificationType.ORDER_COMPLETED, "Message 1", true, null);
        Notification notification2 = new Notification(null, testOrder, testUser, NotificationType.ORDER_EXPIRED, "Message 2", false, null);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // When
        List<Notification> notifications = notificationRepository.findByUserId(testUser.getId());

        // Then
        assertThat(notifications).hasSize(2);
    }

    @Test
    void shouldFindByOrderId() {
        // Given
        Notification notification = new Notification(null, testOrder, testUser, NotificationType.ORDER_COMPLETED, "Message", true, null);
        notificationRepository.save(notification);

        // When
        List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());

        // Then
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getOrder().getId()).isEqualTo(testOrder.getId());
    }

    @Test
    void shouldFindByNotificationType() {
        // Given
        Notification notification1 = new Notification(null, testOrder, testUser, NotificationType.ORDER_COMPLETED, "Message 1", true, null);
        Notification notification2 = new Notification(null, testOrder, testUser, NotificationType.ORDER_EXPIRED, "Message 2", false, null);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // When
        List<Notification> completedNotifications = notificationRepository.findByNotificationType(NotificationType.ORDER_COMPLETED);

        // Then
        assertThat(completedNotifications).hasSize(1);
        assertThat(completedNotifications.get(0).getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
    }
}
```

**Key Points:**
- @DataJpaTest for JPA repository testing
- Uses H2 in-memory database
- Tests all custom query methods
- Creates test data in @BeforeEach
- Cleans up data before each test
- Verifies relationships work correctly

### Task 10: Create Kafka Integration Test

Create `src/test/java/com/example/zadanie/integration/NotificationKafkaIntegrationTest.java`:

```java
package com.example.zadanie.integration;

import com.example.zadanie.entity.*;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.example.zadanie.repository.NotificationRepository;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
import com.example.zadanie.service.OrderEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(topics = {"order-events"}, partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"})
@DirtiesContext
class NotificationKafkaIntegrationTest {

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Clean up
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Create test data
        testUser = new User(null, "Test User", "test@example.com", "password");
        testUser = userRepository.save(testUser);

        testProduct = new Product(null, "Test Product", "Description", new BigDecimal("99.99"), 10, null);
        testProduct = productRepository.save(testProduct);

        testOrder = new Order(null, testUser, testProduct, 2, new BigDecimal("199.98"), OrderStatus.COMPLETED, null, null);
        testOrder = orderRepository.save(testOrder);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateNotificationWhenOrderCompletedEventPublished() {
        // When
        orderEventProducer.publishOrderCompleted(testOrder);

        // Then - wait for async processing (up to 10 seconds)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());
            assertThat(notifications).hasSize(1);

            Notification notification = notifications.get(0);
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
            assertThat(notification.getEmailSent()).isTrue();
            assertThat(notification.getMessage()).contains("completed successfully");
            assertThat(notification.getUser().getId()).isEqualTo(testUser.getId());
        });
    }

    @Test
    void shouldCreateNotificationWhenOrderExpiredEventPublished() {
        // Given
        testOrder.setStatus(OrderStatus.EXPIRED);
        testOrder = orderRepository.save(testOrder);

        // When
        orderEventProducer.publishOrderExpired(testOrder);

        // Then - wait for async processing (up to 10 seconds)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());
            assertThat(notifications).hasSize(1);

            Notification notification = notifications.get(0);
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_EXPIRED);
            assertThat(notification.getEmailSent()).isFalse(); // No email for expired
            assertThat(notification.getMessage()).contains("expired");
            assertThat(notification.getUser().getId()).isEqualTo(testUser.getId());
        });
    }

    @Test
    void shouldNotCreateNotificationForOrderCreatedEvent() {
        // When
        orderEventProducer.publishOrderCreated(testOrder);

        // Then - wait 3 seconds (should not create notification)
        await().pollDelay(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());
            assertThat(notifications).isEmpty(); // NotificationEventConsumer ignores order-created
        });
    }
}
```

**Key Points:**
- @SpringBootTest for full application context
- @EmbeddedKafka on port 9093 (different from production 9092)
- @DirtiesContext ensures clean context per test
- Uses Awaitility for async assertions (waits up to 10 seconds)
- Tests OrderCompleted event creates notification with email
- Tests OrderExpired event creates notification without email
- Tests OrderCreated event is ignored by NotificationEventConsumer
- Cleans up data before and after tests

**Add Awaitility Dependency:**

Add to `pom.xml` in the dependencies section (after existing test dependencies):

```xml
<!-- Awaitility for async testing -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

## Gotchas and Key Considerations

### 1. Separate Consumer Groups for Different Purposes

**Issue:** OrderEventConsumer (payment processor) and NotificationEventConsumer both listen to "order-events".

**Solution:** Use different consumer group IDs:
- OrderEventConsumer: `groupId = "zadanie-order-group"`
- NotificationEventConsumer: `groupId = "zadanie-notification-group"`

**Why:** Different consumer groups process same messages independently. Payment processor needs OrderCreatedEvent, notification consumer needs OrderCompleted/Expired events.

### 2. ConsumerRecord<String, String> vs String Parameter

**Issue:** @KafkaListener can receive message as String or ConsumerRecord.

**Solution:** Use `ConsumerRecord<String, String>` to access message key for filtering event types.

**Example:**
```java
@KafkaListener(topics = "order-events", groupId = "notification-group")
public void consume(ConsumerRecord<String, String> record) {
    String key = record.key(); // "order-completed" or "order-expired"
    String message = record.value(); // JSON string
}
```

### 3. Email Sent Flag Update Requires Second Save

**Issue:** Need to update `emailSent` flag after sending email.

**Solution:** Save notification twice:
1. First save: create notification with emailSent = false
2. Send email
3. Second save: update emailSent = true

**Why:** If email sending fails, we have record that email wasn't sent.

### 4. Foreign Key Cascade Delete

**Issue:** What happens to notifications if order or user is deleted?

**Solution:** CASCADE delete in foreign key constraint means notifications are deleted too.

**Alternative:** Use `onDelete: RESTRICT` to prevent deletion if notifications exist.

**Current Choice:** CASCADE (notifications are audit trail, can be deleted with order/user).

### 5. Notification Message Format

**Issue:** How to format notification messages consistently?

**Solution:** Use String.format with clear patterns:
- Completed: "Your order #{id} has been completed successfully. Total: ${amount}"
- Expired: "Your order #{id} has expired due to payment timeout."

**Why:** Consistent formatting makes it easier to parse/display in UI.

### 6. Transaction Boundaries

**Issue:** Where should @Transactional be applied?

**Solution:**
- NotificationService methods: @Transactional (manages notification + email flag)
- NotificationEventConsumer.consumeOrderEvents: @Transactional (ensures atomicity)

**Why:** If service method fails, consumer can retry. If consumer fails, message is not acknowledged.

### 7. Testing Async Kafka Consumers

**Issue:** Kafka consumers process messages asynchronously.

**Solution:** Use Awaitility library to wait for async processing:
```java
await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
    // Assertions here
});
```

**Why:** Assertions may fail if run immediately after publishing event.

### 8. No Notification for OrderCreated Events

**Issue:** NotificationEventConsumer receives all events on "order-events" topic.

**Solution:** Filter by message key:
```java
if ("order-completed".equals(key)) { ... }
else if ("order-expired".equals(key)) { ... }
else { log.debug("Ignoring event with key: {}", key); }
```

**Why:** NotificationEventConsumer only cares about completed/expired, not created.

### 9. Liquibase Migration Must Run Before Application

**Issue:** Application will fail to start if notifications table doesn't exist.

**Solution:** Liquibase runs automatically on application startup (before JPA).

**Verification:**
```bash
./mvnw clean compile
./mvnw spring-boot:run
# Check logs for: "Liquibase update successful"
```

### 10. Email Sent for Completed, Not for Expired

**Issue:** Feature requirement: email only for completed orders.

**Solution:**
- createOrderCompletedNotification: calls sendFakeEmail(), sets emailSent = true
- createOrderExpiredNotification: NO email call, emailSent = false

**Why:** Users don't need email notification for expired orders (system event).

### 11. Fake Email Implementation

**Issue:** Real email sending requires SMTP configuration.

**Solution:** For this feature, use SLF4J logger to simulate email sending.

**Real Implementation:** Would use JavaMailSender:
```java
@Autowired
private JavaMailSender mailSender;

private void sendEmail(String to, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    mailSender.send(message);
}
```

### 12. Notification Table Column Naming

**Issue:** JPA defaults to camelCase → snake_case mapping.

**Solution:** Explicitly specify column names with @Column(name = "...") to match Liquibase migration.

**Example:**
- Java: `notificationType` → DB: `notification_type`
- Java: `emailSent` → DB: `email_sent`

### 13. H2 vs PostgreSQL Type Differences

**Issue:** Tests use H2, production uses PostgreSQL.

**Solution:** Use standard types that work in both:
- BIGINT (not SERIAL)
- VARCHAR (not CHARACTER VARYING)
- BOOLEAN (H2 converts to TINYINT)
- TEXT (H2 converts to CLOB)

**Verification:** Run tests and production to ensure compatibility.

### 14. Consumer Group Persistence

**Issue:** Kafka stores consumer group offsets.

**Behavior:** On restart, notification consumer resumes from last processed message.

**Impact:** If app crashes while processing, message is reprocessed on restart.

**Solution:** For this feature, reprocessing is acceptable (idempotent operation).

**Future Enhancement:** Check if notification already exists before creating.

### 15. Lombok @Data Generates toString with Lazy-Loaded Entities

**Issue:** Calling notification.toString() may trigger lazy-loading of order and user.

**Impact:** LazyInitializationException if called outside transaction.

**Solution:** For logging, use specific fields instead of toString():
```java
log.info("Created notification ID: {} for order: {}", notification.getId(), notification.getOrder().getId());
```

## Quality Checklist

- [ ] Liquibase migration file created: `005-create-notifications-table.yaml`
- [ ] Migration includes notifications table with all columns
- [ ] Migration includes foreign key constraints to orders and users
- [ ] Migration includes rollback strategy
- [ ] Master changelog updated to include new migration
- [ ] NotificationType enum created with ORDER_COMPLETED and ORDER_EXPIRED
- [ ] Notification entity created with JPA annotations
- [ ] Notification entity has @ManyToOne relationships with Order and User
- [ ] Notification entity has @CreationTimestamp for createdAt
- [ ] NotificationRepository created extending JpaRepository
- [ ] Repository has custom query methods (findByUserId, findByOrderId, etc.)
- [ ] NotificationService created with createOrderCompletedNotification method
- [ ] NotificationService has createOrderExpiredNotification method
- [ ] NotificationService has sendFakeEmail method that logs to console
- [ ] createOrderCompletedNotification sets emailSent = true after sending
- [ ] createOrderExpiredNotification sets emailSent = false (no email)
- [ ] NotificationService uses @Transactional annotation
- [ ] NotificationEventConsumer created with @KafkaListener
- [ ] Consumer uses different group ID: "zadanie-notification-group"
- [ ] Consumer receives ConsumerRecord<String, String> to access key
- [ ] Consumer filters events by key ("order-completed", "order-expired")
- [ ] Consumer ignores "order-created" events
- [ ] Consumer calls NotificationService methods
- [ ] Consumer uses @Transactional annotation
- [ ] NotificationServiceTest created with Mockito
- [ ] Tests verify emailSent flag is true for completed, false for expired
- [ ] Tests verify save() called twice for completed (before/after email)
- [ ] Tests verify save() called once for expired (no email)
- [ ] Tests verify exceptions thrown when order/user not found
- [ ] NotificationRepositoryTest created with @DataJpaTest
- [ ] Repository tests verify all custom query methods work
- [ ] NotificationKafkaIntegrationTest created with @EmbeddedKafka
- [ ] Integration test uses Awaitility for async assertions
- [ ] Integration test verifies OrderCompletedEvent creates notification with email
- [ ] Integration test verifies OrderExpiredEvent creates notification without email
- [ ] Integration test verifies OrderCreatedEvent is ignored
- [ ] Awaitility dependency added to pom.xml
- [ ] All validation gates pass (compile, test, package, run)

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Clean and compile the project
./mvnw clean compile

# Expected output: BUILD SUCCESS
# Expected: No compilation errors

# 2. Run Liquibase update to create notifications table
docker compose up -d postgres
./mvnw liquibase:update

# Expected output: Liquibase update successful
# Expected: notifications table created in database

# 3. Verify notifications table exists
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "\d notifications"

# Expected output: Table structure with all columns:
# - id (bigint, primary key)
# - order_id (bigint, foreign key)
# - user_id (bigint, foreign key)
# - notification_type (varchar(30))
# - message (text)
# - email_sent (boolean)
# - created_at (timestamp)

# 4. Run all tests
./mvnw test

# Expected output: All tests pass
# Expected tests:
# - NotificationServiceTest (5 tests)
# - NotificationRepositoryTest (4 tests)
# - NotificationKafkaIntegrationTest (3 tests)
# - Existing tests still pass

# 5. Package the application
./mvnw package

# Expected output: BUILD SUCCESS
# Expected: JAR file created in target/

# 6. Start Kafka (if not already running)
docker compose up -d kafka

# Wait for Kafka to be healthy
docker compose ps

# Expected: kafka container status "healthy"

# 7. Start Spring Boot application
./mvnw spring-boot:run

# Expected output:
# - Application starts on port 8080
# - "Started ZadanieApplication"
# - No Kafka connection errors
# - "Notification consumer received event" appears in logs when events published

# 8. In a new terminal, get JWT token
export TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123"}' | jq -r '.token')

echo $TOKEN

# Expected: JWT token printed

# 9. Create a test order (triggers OrderCreatedEvent)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1,"productId":1,"quantity":2}' \
  -w "\nStatus: %{http_code}\n"

# Expected: 201 CREATED
# Expected in logs:
# - "Published OrderCreatedEvent for order ID: X"
# - "Processing OrderCreatedEvent for order ID: X"
# - "Simulating payment processing..."

# 10. Wait 6 seconds for payment processing
sleep 6

# Check application logs for:
# - If payment successful (50% chance):
#   - "Payment successful for order X"
#   - "Published OrderCompletedEvent for order ID: X"
#   - "Notification consumer received event with key: order-completed"
#   - "SENDING EMAIL NOTIFICATION"
#   - "To: admin@example.com"
#   - "Created notification and sent email for completed order X"

# 11. Verify notification was created (if order completed)
# Get the order ID from previous response
ORDER_ID=1

curl -X GET http://localhost:8080/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.status'

# If status is "COMPLETED", verify notification exists:
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c \
  "SELECT id, order_id, user_id, notification_type, email_sent, LEFT(message, 50) as message_preview FROM notifications WHERE order_id = $ORDER_ID;"

# Expected output: One row with:
# - notification_type: ORDER_COMPLETED
# - email_sent: true
# - message_preview: "Your order #X has been completed successfully..."

# 12. Test order expiration (manually trigger)
# Create an order and manually set it to PROCESSING and old timestamp
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1,"productId":1,"quantity":1}' | jq '.id'

# Get the new order ID (e.g., 2)
NEW_ORDER_ID=2

# Manually update order to PROCESSING with old timestamp
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c \
  "UPDATE orders SET status = 'PROCESSING', created_at = NOW() - INTERVAL '11 minutes' WHERE id = $NEW_ORDER_ID;"

# Wait for scheduled job to run (up to 60 seconds)
# Check logs for:
# - "Running order expiration job..."
# - "Found 1 orders to expire"
# - "Published OrderExpiredEvent for order ID: X"
# - "Notification consumer received event with key: order-expired"
# - "Created notification for expired order X"

# 13. Verify expired order notification
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c \
  "SELECT id, order_id, user_id, notification_type, email_sent, LEFT(message, 50) as message_preview FROM notifications WHERE order_id = $NEW_ORDER_ID;"

# Expected output: One row with:
# - notification_type: ORDER_EXPIRED
# - email_sent: false (no email for expired)
# - message_preview: "Your order #X has expired due to payment timeout..."

# 14. Verify order status is EXPIRED
curl -X GET http://localhost:8080/api/orders/$NEW_ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.status'

# Expected: "EXPIRED"

# 15. Check all notifications
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c \
  "SELECT id, order_id, notification_type, email_sent, created_at FROM notifications ORDER BY created_at DESC;"

# Expected: List of all notifications (completed and expired)

# 16. Verify Kafka events
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --max-messages 20 \
  --timeout-ms 5000

# Expected: JSON messages including:
# - OrderCreatedEvent (key: "order-created")
# - OrderCompletedEvent (key: "order-completed")
# - OrderExpiredEvent (key: "order-expired")

# 17. Test notification consumer is independent
# Stop application, start again - notifications consumer should resume from last offset
# Create another completed order and verify notification is created

# 18. Stop application and containers
docker compose down

# Expected: All containers stopped and removed
```

## Confidence Score: 8/10

**Rationale:**

**Strong Foundation (+):**
- Kafka infrastructure already implemented and working
- OrderEventProducer already publishes OrderCompleted and OrderExpired events
- Entity, Repository, Service patterns well-established in codebase
- Liquibase migration pattern already in use (3 existing migrations)
- Integration test pattern with @EmbeddedKafka already exists
- All necessary dependencies already in project (Kafka, JPA, Lombok, Jackson)

**Comprehensive PRP (+):**
- Complete code for all 6 new files (enum, entity, repository, service, consumer, tests)
- Detailed Liquibase migration with foreign keys and rollback
- 3 comprehensive test classes (unit, integration, Kafka)
- 18 executable validation gates with expected outputs
- Extensive logging for debugging
- 15 gotchas with solutions

**Clear Requirements (+):**
- Simple feature: listen to events, create notifications, log fake emails
- No complex business logic (just store data and log)
- No new infrastructure needed (Kafka already running)
- No external dependencies (fake email = logging)

**Risk Factors (-):**

1. **Multiple Consumer Groups Pattern (-0.5)**
   - First time using different consumer groups on same topic
   - Must ensure correct group IDs ("zadanie-order-group" vs "zadanie-notification-group")
   - Easy to accidentally use same group (only one consumer would process)
   - Mitigation: Detailed code snippets with correct group IDs

2. **ConsumerRecord<String, String> Parameter (-0.3)**
   - Different from existing OrderEventConsumer (uses String parameter)
   - Must remember to use ConsumerRecord to access message key
   - Easy to forget and just use String parameter (can't filter by key)
   - Mitigation: Complete consumer code provided, explains key filtering

3. **Foreign Key Relationships (-0.4)**
   - First entity with multiple @ManyToOne relationships
   - Lazy loading can cause LazyInitializationException if not careful
   - Must ensure entities are loaded within transaction
   - Mitigation: Gotcha #15 explains toString() pitfall, tests verify loading works

4. **Two-Step Save for Email Flag (-0.3)**
   - Notification saved twice: before email (emailSent=false), after email (emailSent=true)
   - Easy to forget second save
   - Tests must verify save() called twice for completed, once for expired
   - Mitigation: Complete service code shows pattern, tests verify

5. **Async Testing Complexity (-0.5)**
   - Kafka consumers process asynchronously
   - Tests need Awaitility to wait for processing
   - New dependency (Awaitility) must be added to pom.xml
   - Timeout values (10 seconds) may need adjustment
   - Mitigation: Complete integration test with Awaitility, pom.xml snippet provided

**Strengths:**
- Builds on existing, working Kafka infrastructure
- Simple domain model (just notifications, no complex business logic)
- Fake email = logging (no SMTP configuration needed)
- No schema changes to existing tables
- Clear separation of concerns (service creates, consumer listens)
- Comprehensive tests (unit + integration + Kafka)

**Weaknesses:**
- Multiple consumer groups concept is new
- ConsumerRecord parameter different from existing pattern
- Async testing requires new dependency (Awaitility)
- Two-step save pattern is subtle

**Expected Outcome:**
An AI agent with access to this PRP, the codebase, and web search should successfully implement the notification system with high confidence. The score of 8/10 reflects:
- Strong foundation (Kafka already working)
- Simple requirements (store notifications, log emails)
- Comprehensive guidance (all code provided)
- Minor risks (consumer groups, async testing)

Implementation should be straightforward following the detailed tasks, with main challenges being:
1. Using correct consumer group ID (different from OrderEventConsumer)
2. Using ConsumerRecord<String, String> parameter to access key
3. Remembering two-step save for email flag
4. Using Awaitility for async testing

**Recommendation for AI Agent:**
1. Implement tasks sequentially (Liquibase → Entity → Repository → Service → Consumer → Tests)
2. Verify Liquibase migration creates table before proceeding
3. Test service methods independently before testing Kafka consumer
4. Use extensive logging to debug async Kafka processing
5. Verify validation gates incrementally (don't wait until end)
6. Pay attention to consumer group ID (must be different from OrderEventConsumer)

## Documentation Sources

This PRP was created using research from the following sources:

**Spring Kafka:**
- [Multiple Event Types in the Same Kafka Topic - Confluent](https://www.confluent.io/blog/multiple-event-types-in-the-same-kafka-topic/)
- [Spring Kafka: Configure Multiple Listeners on Same Topic - Baeldung](https://www.baeldung.com/spring-kafka-multiple-listeners-same-topic)
- [Spring Boot Kafka Multiple Consumers Example - HowToDoInJava](https://howtodoinjava.com/kafka/multiple-consumers-example/)
- [Spring Kafka Reference Documentation](https://docs.spring.io/spring-kafka/reference/html/)

**Spring Boot Email:**
- [Building Email Notification Systems in Spring Boot - DEV Community](https://dev.to/ayshriv/building-synchronous-email-notification-systems-in-spring-boot-a-step-by-step-guide-1lik)
- [How to Set Up Email Notifications in Spring Boot - Medium](https://medium.com/@AlexanderObregon/how-to-set-up-email-notifications-in-spring-boot-applications-b5a2574c5e8f)
- [Guide to Spring Email - Baeldung](https://www.baeldung.com/spring-email)
- [Sending Emails in Spring Boot 2024 - Mailtrap](https://mailtrap.io/blog/spring-send-email/)

**Spring Data JPA:**
- [Spring Data JPA Reference Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JPA Entity Mapping - Jakarta Persistence](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)

**Liquibase:**
- [Liquibase YAML Format](https://docs.liquibase.com/concepts/changelogs/yaml-format.html)
- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html)