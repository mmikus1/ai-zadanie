# PRP: Orders Module with Relationships, Enum, Stock Management, and JWT Authentication

## Context and Overview

This PRP defines the implementation of a complete Orders module for the existing Spring Boot application. This module is MORE COMPLEX than the existing Products and Users modules because it introduces:
1. **First enum in the project** (OrderStatus)
2. **Entity relationships** (@ManyToOne to User and Product)
3. **Complex transactional logic** (stock validation and decrement)
4. **Business rules** (status-based update restrictions)
5. **Auto-calculated fields** (total price)

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database (already configured and running)
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction
- JWT authentication already implemented with Spring Security

**Feature Requirements:**
1. Create OrderStatus enum (PENDING, PROCESSING, COMPLETED, EXPIRED) - **FIRST ENUM IN PROJECT**
2. Create Order entity with relationships to User and Product (@ManyToOne)
3. Create OrderRepository extending JpaRepository
4. Create OrderService with complex business logic:
   - Stock validation before order creation
   - Stock decrement in same transaction
   - Auto-calculate total price
   - Prevent updates to completed/expired orders
5. Create OrderController with 5 REST endpoints (GET all, GET by ID, POST create, PUT update, DELETE)
6. Create DTOs: OrderCreateRequest, OrderUpdateRequest
7. Protect all order endpoints with JWT authentication
8. Add comprehensive validation on request data
9. Create full test suite (controller, service, repository tests)

**Existing Codebase Context:**
- User entity exists at `src/main/java/com/example/zadanie/entity/User.java:1-36`
- Product entity exists at `src/main/java/com/example/zadanie/entity/Product.java:1-48` (has stock field)
- ProductService at `src/main/java/com/example/zadanie/service/ProductService.java:1-72`
- ProductRepository at `src/main/java/com/example/zadanie/repository/ProductRepository.java:1-20`
- ProductController pattern at `src/main/java/com/example/zadanie/api/controller/ProductController.java:1-73`
- ProductCreateRequest DTO at `src/main/java/com/example/zadanie/api/dto/ProductCreateRequest.java:1-31`
- ProductUpdateRequest DTO at `src/main/java/com/example/zadanie/api/dto/ProductUpdateRequest.java:1-27`
- SecurityConfig with JWT filter at `src/main/java/com/example/zadanie/config/SecurityConfig.java:1-67`
- Test patterns at `src/test/java/com/example/zadanie/controller/ProductControllerTest.java:1-165`
- ProductServiceTest at `src/test/java/com/example/zadanie/service/ProductServiceTest.java`
- ProductRepositoryTest at `src/test/java/com/example/zadanie/repository/ProductRepositoryTest.java`

## Research Findings and Documentation

### Java Enums with JPA (NEW - No enums exist in project yet)

**Critical Resources:**
- [Enum Types in Java - Oracle Official Tutorial](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html)
- [Using Enums with JPA - Baeldung](https://www.baeldung.com/jpa-persisting-enums-in-jpa)
- [EnumType.STRING vs EnumType.ORDINAL - Vlad Mihalcea](https://vladmihalcea.com/the-best-way-to-map-an-enum-type-with-jpa-and-hibernate/)
- [JPA @Enumerated Annotation - JPA Spec](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#a385)

**Key Points:**
- Create enum as separate class in entity package
- Use uppercase values: PENDING, PROCESSING, COMPLETED, EXPIRED
- In entity, use `@Enumerated(EnumType.STRING)` NOT EnumType.ORDINAL
- STRING stores enum name in DB (e.g., "PENDING"), ORDINAL stores integer
- ORDINAL is dangerous - if enum order changes, data corruption occurs
- Validation: Use enum directly in DTO, Spring validates automatically
- Database column will be VARCHAR(20) for enum

### JPA Entity Relationships (@ManyToOne)

**Critical Resources:**
- [@ManyToOne and @JoinColumn - Baeldung](https://www.baeldung.com/jpa-join-column)
- [JPA Entity Relationships - Spring Data JPA Docs](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Hibernate Many-to-One Mapping](https://www.baeldung.com/hibernate-one-to-many)
- [JPA FetchType - Oracle](https://docs.oracle.com/javaee/7/api/javax/persistence/FetchType.html)

**Key Points:**
- Use `@ManyToOne` for Order -> User relationship (many orders belong to one user)
- Use `@ManyToOne` for Order -> Product relationship (many orders reference one product)
- Add `@JoinColumn(name = "user_id", nullable = false)` to specify FK column name
- Add `@JoinColumn(name = "product_id", nullable = false)` to specify FK column name
- Default fetch type for @ManyToOne is EAGER (loads related entity immediately)
- Consider `fetch = FetchType.LAZY` for performance (loads only when accessed)
- Foreign key constraints auto-created by Hibernate

### Hibernate Timestamps (@UpdateTimestamp is NEW)

**Critical Resources:**
- [Hibernate @CreationTimestamp and @UpdateTimestamp | Baeldung](https://www.baeldung.com/hibernate-creationtimestamp-updatetimestamp)
- [Hibernate @CreationTimestamp Tutorial - JavaGuides](https://www.javaguides.net/2024/05/hibernate-creationtimestamp-and-updatetimestamp.html)
- [Persist Creation and Update Timestamps - Thorben Janssen](https://thorben-janssen.com/persist-creation-update-timestamps-hibernate/)
- [Entity Create and Update Timestamps - HowToDoInJava](https://howtodoinjava.com/spring-data/entity-create-and-update-timestamps/)

**Key Points:**
- Use `@CreationTimestamp` for created_at field (auto-populated on INSERT)
- Use `@UpdateTimestamp` for updated_at field (auto-populated on UPDATE) - **NEW in project**
- Use `LocalDateTime` type (NOT Date or Timestamp)
- Add `@Column(updatable = false)` to created_at to prevent updates
- Import from `org.hibernate.annotations.CreationTimestamp` and `org.hibernate.annotations.UpdateTimestamp`
- Both annotations are Hibernate-specific (not JPA standard)

### Spring Transactions (@Transactional)

**Critical Resources:**
- [Spring @Transactional - Baeldung](https://www.baeldung.com/transaction-configuration-with-jpa-and-spring)
- [Understanding Spring Transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Spring Transaction Management Best Practices](https://www.marcobehler.com/guides/spring-transaction-management-unconventional-guide)

**Key Points:**
- OrderService.createOrder MUST be @Transactional
- Transaction ensures atomicity: either ALL operations succeed or ALL rollback
- If stock check fails, throw exception to rollback transaction
- Multiple repository saves in single transaction are committed together
- Service layer is correct place for @Transactional, NOT repository or controller
- Default propagation is REQUIRED (joins existing transaction or creates new one)

### BigDecimal Arithmetic

**Critical Resources:**
- [BigDecimal Best Practices - Baeldung](https://www.baeldung.com/java-bigdecimal-biginteger)
- [JPA Column Precision Scale Example](http://www.java2s.com/Tutorials/Java/JPA/0140__JPA_Column_Precision_Scale.htm)
- [BigDecimal Precision Guide](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

**Key Points:**
- Use BigDecimal for monetary values (total)
- `@Column(precision = 10, scale = 2)` like Product.price
- For multiplication: `price.multiply(BigDecimal.valueOf(quantity))`
- NEVER use double constructor: `new BigDecimal(99.99)` - causes precision errors
- Always use string constructor: `new BigDecimal("99.99")`
- For quantity conversion: `BigDecimal.valueOf(intValue)` is safe

### Bean Validation

**Critical Resources:**
- [Jakarta Bean Validation 3.0](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Hibernate Validator](https://hibernate.org/validator/)
- [Spring Boot Validation Guide](https://spring.io/guides/gs/validating-form-input)

**Key Points:**
- Use `@NotNull` for required fields in OrderCreateRequest
- Use `@Min(0)` for quantity (must be non-negative integer)
- Use `@DecimalMin("0.0")` for total (must be non-negative decimal)
- NO @NotNull in OrderUpdateRequest (partial updates)
- Enum validation automatic (Spring validates enum values)
- Use `@Valid` in controller to trigger validation

### Spring Boot REST with Spring Security

**Critical Resources:**
- [Spring Boot 3.5 Reference](https://docs.spring.io/spring-boot/reference/index.html)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Building REST Services with Spring](https://spring.io/guides/tutorials/rest)
- [Spring Security 6 Documentation](https://docs.spring.io/spring-security/reference/index.html)

**Key Points:**
- Order endpoints MUST be protected with JWT authentication
- Update SecurityConfig to add `.requestMatchers("/api/orders/**").authenticated()`
- Controller tests need to exclude JwtAuthenticationFilter and use TestSecurityConfig
- Tests need `.with(csrf())` for POST/PUT/DELETE requests

### Testing with Spring Boot

**Critical Resources:**
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [@WebMvcTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)
- [@DataJpaTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)
- [Mockito Framework](https://site.mockito.org/)

**Key Points:**
- Use `@WebMvcTest` for controller tests with MockMvc
- Use `@DataJpaTest` for repository tests with H2 database
- Use `@ExtendWith(MockitoExtension.class)` for service unit tests
- Follow ProductControllerTest pattern for JWT security configuration in tests
- Mock ProductRepository and UserRepository in OrderServiceTest
- Test stock validation and update restriction business rules

## Implementation Blueprint

### High-Level Approach

```
1. Enum Layer (NEW)
   ├── Create OrderStatus enum with 4 values
   └── Use @Enumerated(EnumType.STRING) in Order entity

2. Entity Layer
   ├── Create Order entity with all required fields
   ├── Add @ManyToOne relationships to User and Product
   ├── Use BigDecimal for total
   ├── Use Integer for quantity
   ├── Use LocalDateTime with @CreationTimestamp for created_at
   ├── Use LocalDateTime with @UpdateTimestamp for updated_at
   └── Add field-level validation annotations

3. Repository Layer
   ├── Create OrderRepository interface
   ├── Extend JpaRepository<Order, Long>
   └── Add custom query methods: findByUserId, findByStatus

4. DTO Layer
   ├── Create OrderCreateRequest with userId, productId, quantity
   ├── Create OrderUpdateRequest with optional quantity and status
   ├── Add validation annotations to DTOs
   └── DO NOT include total in DTOs (security - calculated server-side)

5. Service Layer (COMPLEX)
   ├── Create OrderService with @Service annotation
   ├── Inject ProductRepository, UserRepository, OrderRepository
   ├── Implement getAllOrders()
   ├── Implement getOrderById()
   ├── Implement createOrder() with:
   │   ├── Validate user exists
   │   ├── Validate product exists
   │   ├── Check stock >= quantity and stock > 0
   │   ├── Calculate total = product.price * quantity
   │   ├── Decrement product.stock
   │   ├── Save product (stock update)
   │   ├── Save order
   │   └── All in single @Transactional method
   ├── Implement updateOrder() with:
   │   ├── Check status not COMPLETED or EXPIRED
   │   ├── Update only provided fields
   │   └── updatedAt auto-updated by @UpdateTimestamp
   └── Implement deleteOrder()

6. Controller Layer
   ├── Create OrderController with @RestController
   ├── Implement 5 CRUD endpoints
   ├── Add @Valid for request validation
   ├── Add OpenAPI/Swagger annotations
   └── Handle exceptions with try-catch

7. Security Configuration
   ├── Update SecurityConfig.java
   └── Add .requestMatchers("/api/orders/**").authenticated()

8. Testing
   ├── Create OrderRepositoryTest with @DataJpaTest
   ├── Create OrderServiceTest with Mockito
   │   ├── Test stock validation failure
   │   ├── Test update restriction when completed/expired
   │   ├── Mock ProductRepository and UserRepository
   ├── Create OrderControllerTest with @WebMvcTest
   └── Follow ProductControllerTest pattern for security setup

9. Validation & Integration
   ├── Compile and run tests
   ├── Test with curl commands
   └── Verify Swagger UI integration
```

### Package Structure

```
com.example.zadanie/
├── entity/
│   ├── Order.java (NEW)
│   ├── OrderStatus.java (NEW - enum)
│   ├── Product.java (EXISTING - no changes)
│   └── User.java (EXISTING - no changes)
├── repository/
│   ├── OrderRepository.java (NEW)
│   ├── ProductRepository.java (EXISTING - no changes)
│   └── UserRepository.java (EXISTING - no changes)
├── service/
│   ├── OrderService.java (NEW)
│   └── ProductService.java (EXISTING - no changes)
├── api/
│   ├── controller/
│   │   └── OrderController.java (NEW)
│   └── dto/
│       ├── OrderCreateRequest.java (NEW)
│       └── OrderUpdateRequest.java (NEW)
└── config/
    └── SecurityConfig.java (EXISTING - UPDATE line 41)
```

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Create OrderStatus Enum

Create `src/main/java/com/example/zadanie/entity/OrderStatus.java`:

```java
package com.example.zadanie.entity;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    EXPIRED
}
```

**Key Points:**
- Simple enum with 4 uppercase values
- No annotations needed on enum itself
- Place in entity package (enums are domain types)
- This is the FIRST enum in the project

### Task 2: Create Order Entity

Create `src/main/java/com/example/zadanie/entity/Order.java`:

```java
package com.example.zadanie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Total is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total must be greater than or equal to 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

**Key Points:**
- Entity name: `Order` (singular), table name: `orders` (plural)
- Uses Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` matching Product pattern
- `@ManyToOne(fetch = FetchType.LAZY)` for relationships (LAZY to avoid N+1 queries)
- `@JoinColumn(name = "user_id")` creates foreign key column
- `@Enumerated(EnumType.STRING)` stores enum name as string in DB
- BigDecimal for total with precision=10, scale=2 (like Product.price)
- `@UpdateTimestamp` is NEW - auto-updates on every UPDATE
- `@Column(updatable = false)` on createdAt prevents modification
- `@Column(length = 20)` for status enum VARCHAR

### Task 3: Create Order Repository

Create `src/main/java/com/example/zadanie/repository/OrderRepository.java`:

```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // JpaRepository provides:
    // - save(Order order)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - existsById(Long id)

    // Custom query methods
    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByProductId(Long productId);
}
```

**Key Points:**
- Extends `JpaRepository<Order, Long>` (entity type, ID type)
- Basic CRUD methods provided automatically by Spring Data JPA
- Custom query methods derived from method names
- `findByUserId` queries orders.user_id column
- `findByStatus` filters by enum value

### Task 4: Create OrderCreateRequest DTO

Create `src/main/java/com/example/zadanie/api/dto/OrderCreateRequest.java`:

```java
package com.example.zadanie.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // NOTE: total is NOT in DTO - calculated server-side for security
    // NOTE: status is NOT in DTO - defaults to PENDING
}
```

**Key Points:**
- Only userId, productId, quantity from client
- NO total field (security risk - server calculates)
- NO status field (defaults to PENDING on creation)
- All fields required with @NotNull
- Follows ProductCreateRequest pattern

### Task 5: Create OrderUpdateRequest DTO

Create `src/main/java/com/example/zadanie/api/dto/OrderUpdateRequest.java`:

```java
package com.example.zadanie.api.dto;

import com.example.zadanie.entity.OrderStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private OrderStatus status;

    // NOTE: All fields optional for partial update
    // NOTE: Cannot update userId or productId (order immutable once created)
    // NOTE: total is recalculated if quantity changes
}
```

**Key Points:**
- All fields optional (no @NotNull) for partial updates
- Can update quantity or status
- NO userId or productId (orders can't change user/product after creation)
- Enum validation automatic (Spring validates enum values)
- Follows ProductUpdateRequest pattern

### Task 6: Create OrderService

Create `src/main/java/com/example/zadanie/service/OrderService.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.api.dto.OrderCreateRequest;
import com.example.zadanie.api.dto.OrderUpdateRequest;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

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
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(Long id, OrderUpdateRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        // Check if order can be updated (not completed or expired)
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot update order with status: " + order.getStatus());
        }

        // Update quantity if provided
        if (request.getQuantity() != null && !request.getQuantity().equals(order.getQuantity())) {
            // Recalculate total if quantity changed
            BigDecimal newTotal = order.getProduct().getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            order.setQuantity(request.getQuantity());
            order.setTotal(newTotal);
            // Note: Stock management for updates is complex - simplified here
            // In production, you'd need to adjust stock: restore old quantity, check new quantity
        }

        // Update status if provided
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        // updatedAt auto-updated by @UpdateTimestamp
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found with id: " + id);
        }
        // Note: In production, you might want to restore stock when deleting pending orders
        orderRepository.deleteById(id);
    }
}
```

**Key Points:**
- `@Service` for Spring service bean
- `@RequiredArgsConstructor` for constructor injection (3 repositories)
- `@Transactional` on write operations (create, update, delete)
- createOrder is complex:
  1. Validates user and product exist
  2. Checks stock availability
  3. Calculates total
  4. Decrements stock
  5. Saves both product and order in same transaction
- updateOrder checks status restrictions
- Throws `IllegalArgumentException` for not found or business rule violations
- updatedAt auto-updated by @UpdateTimestamp annotation

### Task 7: Create OrderController

Create `src/main/java/com/example/zadanie/api/controller/OrderController.java`:

```java
package com.example.zadanie.api.controller;

import com.example.zadanie.api.dto.OrderCreateRequest;
import com.example.zadanie.api.dto.OrderUpdateRequest;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves a list of all orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by ID")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user", description = "Retrieves all orders for a specific user")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves all orders with a specific status")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Creates a new order")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Updates an existing order")
    public ResponseEntity<Order> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request) {
        try {
            Order order = orderService.updateOrder(id, request);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Deletes an order by ID")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Key Points:**
- `@RestController` and `@RequestMapping("/api/orders")`
- `@RequiredArgsConstructor` for dependency injection
- OpenAPI annotations: `@Tag` for controller, `@Operation` for each endpoint
- `@Valid` on POST and PUT to trigger validation
- 7 endpoints (5 standard CRUD + 2 query endpoints)
- Returns appropriate HTTP status codes:
  - 200 OK for GET, PUT
  - 201 CREATED for POST
  - 204 NO_CONTENT for DELETE
  - 404 NOT_FOUND when order doesn't exist
  - 400 BAD_REQUEST for validation errors
- Follows ProductController pattern exactly

### Task 8: Update SecurityConfig to Protect Order Endpoints

Update `src/main/java/com/example/zadanie/config/SecurityConfig.java`:

Find the `authorizeHttpRequests` section (around line 37-42) and add order endpoints:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .requestMatchers("/api/users/**").authenticated()
        .requestMatchers("/api/products/**").authenticated()
        .requestMatchers("/api/orders/**").authenticated()  // ADD THIS LINE
        .anyRequest().authenticated()
)
```

**Location:** Add after line 41 in `SecurityConfig.java`

**Key Points:**
- All order endpoints require JWT authentication
- Unauthenticated requests will get 401 UNAUTHORIZED
- JWT token must be sent in Authorization header: `Bearer {token}`

### Task 9: Create OrderRepositoryTest

Create `src/test/java/com/example/zadanie/repository/OrderRepositoryTest.java`:

```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void shouldSaveAndFindOrder() {
        // Given
        Order order = new Order();
        order.setUser(testUser);
        order.setProduct(testProduct);
        order.setQuantity(2);
        order.setTotal(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.PENDING);

        // When
        Order saved = orderRepository.save(order);
        Optional<Order> found = orderRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
        assertThat(found.get().getTotal()).isEqualByComparingTo(new BigDecimal("199.98"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindOrdersByUserId() {
        // Given
        Order order1 = new Order();
        order1.setUser(testUser);
        order1.setProduct(testProduct);
        order1.setQuantity(1);
        order1.setTotal(new BigDecimal("99.99"));
        order1.setStatus(OrderStatus.PENDING);
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setUser(testUser);
        order2.setProduct(testProduct);
        order2.setQuantity(2);
        order2.setTotal(new BigDecimal("199.98"));
        order2.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order2);

        // When
        List<Order> orders = orderRepository.findByUserId(testUser.getId());

        // Then
        assertThat(orders).hasSize(2);
    }

    @Test
    void shouldFindOrdersByStatus() {
        // Given
        Order order1 = new Order();
        order1.setUser(testUser);
        order1.setProduct(testProduct);
        order1.setQuantity(1);
        order1.setTotal(new BigDecimal("99.99"));
        order1.setStatus(OrderStatus.PENDING);
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setUser(testUser);
        order2.setProduct(testProduct);
        order2.setQuantity(2);
        order2.setTotal(new BigDecimal("199.98"));
        order2.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order2);

        // When
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(pendingOrders).hasSize(1);
        assertThat(completedOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        // When
        Optional<Order> found = orderRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }
}
```

**Key Points:**
- `@DataJpaTest` for repository testing with H2 in-memory database
- Sets up test data in @BeforeEach (User and Product required for Order)
- Tests basic CRUD operations
- Tests custom query methods: findByUserId, findByStatus
- Uses BigDecimal comparison with `isEqualByComparingTo()`
- Verifies createdAt and updatedAt are auto-populated
- Tests enum persistence and retrieval

### Task 10: Create OrderServiceTest

Create `src/test/java/com/example/zadanie/service/OrderServiceTest.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.api.dto.OrderCreateRequest;
import com.example.zadanie.api.dto.OrderUpdateRequest;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

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
        testProduct.setStock(10);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldGetAllOrders() {
        // Given
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder));

        // When
        List<Order> orders = orderService.getAllOrders();

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getQuantity()).isEqualTo(2);
        verify(orderRepository).findAll();
    }

    @Test
    void shouldGetOrderById() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Optional<Order> found = orderService.getOrderById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
        verify(orderRepository).findById(1L);
    }

    @Test
    void shouldCreateOrder() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order created = orderService.createOrder(request);

        // Then
        assertThat(created).isNotNull();
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct); // Stock updated
        verify(orderRepository).save(any(Order.class));
        assertThat(testProduct.getStock()).isEqualTo(8); // 10 - 2 = 8
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(999L, 1L, 2);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(1L, 999L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void shouldThrowExceptionWhenStockInsufficient() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 20); // Request 20, but only 10 in stock
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldThrowExceptionWhenStockIsZero() {
        // Given
        testProduct.setStock(0);
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("out of stock");
    }

    @Test
    void shouldUpdateOrder() {
        // Given
        OrderUpdateRequest request = new OrderUpdateRequest(3, OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order updated = orderService.updateOrder(1L, request);

        // Then
        assertThat(updated).isNotNull();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingCompletedOrder() {
        // Given
        testOrder.setStatus(OrderStatus.COMPLETED);
        OrderUpdateRequest request = new OrderUpdateRequest(3, null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot update order with status: COMPLETED");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingExpiredOrder() {
        // Given
        testOrder.setStatus(OrderStatus.EXPIRED);
        OrderUpdateRequest request = new OrderUpdateRequest(3, null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot update order with status: EXPIRED");
    }

    @Test
    void shouldDeleteOrder() {
        // Given
        when(orderRepository.existsById(1L)).thenReturn(true);

        // When
        orderService.deleteOrder(1L);

        // Then
        verify(orderRepository).existsById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentOrder() {
        // Given
        when(orderRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> orderService.deleteOrder(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order not found");
    }
}
```

**Key Points:**
- `@ExtendWith(MockitoExtension.class)` for Mockito support
- `@Mock` for all three repository dependencies
- `@InjectMocks` for service under test
- Tests all service methods
- **Critical tests for business rules:**
  - Stock validation (insufficient, zero)
  - User/Product not found
  - Update restriction when completed/expired
- Verifies stock decrement: `assertThat(testProduct.getStock()).isEqualTo(8)`
- Uses `verify()` to check repository method calls

### Task 11: Create OrderControllerTest

Create `src/test/java/com/example/zadanie/controller/OrderControllerTest.java`:

```java
package com.example.zadanie.controller;

import com.example.zadanie.api.controller.OrderController;
import com.example.zadanie.api.dto.OrderCreateRequest;
import com.example.zadanie.api.dto.OrderUpdateRequest;
import com.example.zadanie.config.JwtAuthenticationFilter;
import com.example.zadanie.config.TestSecurityConfig;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.service.JwtService;
import com.example.zadanie.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

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
        testProduct.setPrice(new BigDecimal("99.99"));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldGetAllOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(testOrder));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].quantity").value(2))
            .andExpect(jsonPath("$[0].total").value(199.98))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(testOrder));

        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.total").value(199.98));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateOrder() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 2);

        when(orderService.createOrder(any())).thenReturn(testOrder);

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void shouldReturnBadRequestForInvalidOrder() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(null, null, -5);  // Invalid data

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStockInsufficient() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 100);

        when(orderService.createOrder(any()))
            .thenThrow(new IllegalArgumentException("Insufficient stock"));

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateOrder() throws Exception {
        OrderUpdateRequest request = new OrderUpdateRequest(3, OrderStatus.PROCESSING);

        when(orderService.updateOrder(eq(1L), any())).thenReturn(testOrder);

        mockMvc.perform(put("/api/orders/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentOrder() throws Exception {
        OrderUpdateRequest request = new OrderUpdateRequest(3, null);

        when(orderService.updateOrder(eq(999L), any()))
            .thenThrow(new IllegalArgumentException("Order not found"));

        mockMvc.perform(put("/api/orders/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/orders/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentOrder() throws Exception {
        doThrow(new IllegalArgumentException("Order not found"))
            .when(orderService).deleteOrder(999L);

        mockMvc.perform(delete("/api/orders/999")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }
}
```

**Key Points:**
- `@WebMvcTest(OrderController.class)` for controller testing
- Excludes `JwtAuthenticationFilter` to avoid JWT token validation in tests
- Imports `TestSecurityConfig` for test security configuration
- `@MockBean` for OrderService and JwtService
- Uses `.with(csrf())` for POST/PUT/DELETE requests
- Tests all HTTP methods and error scenarios
- Tests business rule failures (insufficient stock)
- Uses `ObjectMapper` to serialize request bodies
- Follows ProductControllerTest pattern exactly

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Clean and compile the project
./mvnw clean compile

# Expected output: BUILD SUCCESS

# 2. Run all tests
./mvnw test

# Expected output: All tests pass (including new Order tests)
# Expected: 39 existing tests + 4 OrderRepositoryTest + 11 OrderServiceTest + 9 OrderControllerTest = 63+ tests

# 3. Package the application
./mvnw package

# Expected output: BUILD SUCCESS, JAR created in target/

# 4. Start the application (ensure PostgreSQL is running)
docker compose up -d  # Start PostgreSQL if not already running
./mvnw spring-boot:run

# Expected output: Application starts on port 8080
# Expected: orders table auto-created in database with foreign keys

# 5. Get JWT token for testing
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"Test@1234"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with JWT token
# Export token: export TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 6. Create a product first (for testing)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Test Product","description":"For testing orders","price":"99.99","stock":10}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 201 CREATED with product JSON (note the id)

# 7. Test Create Order (with JWT token)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1,"productId":1,"quantity":2}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 201 CREATED with order JSON
# Example: {"id":1,"user":{...},"product":{...},"quantity":2,"total":199.98,"status":"PENDING","createdAt":"2026-01-19T...","updatedAt":"2026-01-19T..."}
# NOTE: total is auto-calculated (99.99 * 2 = 199.98)

# 8. Test Get All Orders (with JWT token)
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with array of orders

# 9. Test Get Order by ID (with JWT token)
curl -X GET http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with order JSON

# 10. Test Update Order (with JWT token)
curl -X PUT http://localhost:8080/api/orders/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"quantity":3,"status":"PROCESSING"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with updated order JSON
# NOTE: total recalculated automatically, updatedAt changed

# 11. Test Update Completed Order (should fail)
# First, mark order as completed:
curl -X PUT http://localhost:8080/api/orders/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"COMPLETED"}' \
  -w "\nStatus: %{http_code}\n"

# Then try to update again (should fail):
curl -X PUT http://localhost:8080/api/orders/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"quantity":5}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 404 NOT_FOUND (cannot update completed order)

# 12. Test Insufficient Stock (should fail)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1,"productId":1,"quantity":1000}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 400 BAD_REQUEST (insufficient stock)

# 13. Test Delete Order (with JWT token)
curl -X DELETE http://localhost:8080/api/orders/2 \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nStatus: %{http_code}\n"

# Expected output: 204 NO CONTENT

# 14. Test Unauthorized Access (without JWT token)
curl -X GET http://localhost:8080/api/orders \
  -w "\nStatus: %{http_code}\n"

# Expected output: 401 UNAUTHORIZED

# 15. Verify Swagger UI
# Open browser: http://localhost:8080/swagger-ui.html
# Expected: See "Order Management" section with 7 endpoints

# 16. Verify database table created with relationships
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "\d orders"

# Expected output: Table structure showing columns including user_id and product_id foreign keys

# 17. Query orders from database
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "SELECT * FROM orders;"

# Expected output: Order records with enum stored as string, timestamps, and calculated totals

# 18. Verify product stock was decremented
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "SELECT id, name, stock FROM products WHERE id=1;"

# Expected output: Stock should be less than original (10 - quantity ordered)
```

## Error Handling Strategy

### Validation Errors (400 Bad Request)

**Scenario 1: Missing Required Fields**
- Triggered by: `@NotNull` annotations in OrderCreateRequest
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"userId": "User ID is required", "productId": "Product ID is required", "quantity": "Quantity is required"}`

**Scenario 2: Invalid Field Values**
- Triggered by: `@Min(1)` on quantity
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"quantity": "Quantity must be at least 1"}`

**Scenario 3: Insufficient Stock**
- Triggered by: Business logic in OrderService.createOrder
- Service throws: `IllegalArgumentException("Insufficient stock")`
- Controller catches: Returns `ResponseEntity.badRequest().build()`
- HTTP Status: 400

**Scenario 4: Product Out of Stock**
- Triggered by: Business logic check `product.stock <= 0`
- Service throws: `IllegalArgumentException("Product is out of stock")`
- Controller catches: Returns `ResponseEntity.badRequest().build()`
- HTTP Status: 400

### Resource Not Found (404 Not Found)

**Scenario 5: Order Not Found (GET by ID)**
- Service returns `Optional.empty()`
- Controller returns `ResponseEntity.notFound().build()`
- HTTP Status: 404

**Scenario 6: User Not Found (during order creation)**
- Service throws `IllegalArgumentException("User not found with id: X")`
- Controller catches exception and returns `ResponseEntity.badRequest().build()`
- HTTP Status: 400

**Scenario 7: Product Not Found (during order creation)**
- Service throws `IllegalArgumentException("Product not found with id: X")`
- Controller catches exception and returns `ResponseEntity.badRequest().build()`
- HTTP Status: 400

**Scenario 8: Order Not Found (UPDATE)**
- Service throws `IllegalArgumentException("Order not found with id: X")`
- Controller catches exception and returns `ResponseEntity.notFound().build()`
- HTTP Status: 404

**Scenario 9: Order Not Found (DELETE)**
- Service throws `IllegalArgumentException("Order not found with id: X")`
- Controller catches exception and returns `ResponseEntity.notFound().build()`
- HTTP Status: 404

### Business Rule Violations (404 Not Found or 400 Bad Request)

**Scenario 10: Cannot Update Completed Order**
- Triggered by: Status check in OrderService.updateOrder
- Service throws: `IllegalArgumentException("Cannot update order with status: COMPLETED")`
- Controller catches: Returns `ResponseEntity.notFound().build()`
- HTTP Status: 404

**Scenario 11: Cannot Update Expired Order**
- Triggered by: Status check in OrderService.updateOrder
- Service throws: `IllegalArgumentException("Cannot update order with status: EXPIRED")`
- Controller catches: Returns `ResponseEntity.notFound().build()`
- HTTP Status: 404

### Authentication Errors (401 Unauthorized)

**Scenario 12: Missing JWT Token**
- Spring Security JwtAuthenticationFilter intercepts request
- No token in Authorization header
- HTTP Status: 401

**Scenario 13: Invalid JWT Token**
- Spring Security JwtAuthenticationFilter validates token
- Token is expired, malformed, or has invalid signature
- HTTP Status: 401

## Gotchas and Critical Considerations

### 1. Enum Implementation (FIRST IN PROJECT)

**CRITICAL:** This is the first enum in the project.

**Problem:**
- No existing enum pattern to follow
- Easy to use EnumType.ORDINAL instead of EnumType.STRING
- Ordinal causes data corruption if enum order changes

**Solution:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private OrderStatus status;
```

**Important:**
- ALWAYS use EnumType.STRING (stores "PENDING", "PROCESSING", etc.)
- NEVER use EnumType.ORDINAL (stores 0, 1, 2, 3)
- Enum values must be uppercase: PENDING, not pending
- Column length 20 is sufficient for all enum values
- Validation automatic - Spring validates enum values in DTO

### 2. Entity Relationships (@ManyToOne)

**CRITICAL:** First use of JPA relationships in project.

**Problem:**
- Easy to forget @JoinColumn or use wrong column names
- Default EAGER fetching can cause N+1 query problems
- Foreign key constraints not obvious in code

**Solution:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@NotNull(message = "User is required")
private User user;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", nullable = false)
@NotNull(message = "Product is required")
private Product product;
```

**Important:**
- `@JoinColumn(name = "user_id")` creates column in orders table
- Foreign key constraint auto-created by Hibernate
- `fetch = FetchType.LAZY` loads related entity only when accessed
- Both @NotNull and `nullable = false` for consistency
- In tests, must set up User and Product before creating Order

### 3. Stock Management (MOST CRITICAL)

**CRITICAL:** Transactional integrity is essential.

**Problem:**
- Race condition: two orders can check stock simultaneously
- If transaction fails after stock update, stock corrupted
- Easy to forget to save product after updating stock

**Solution:**
```java
@Transactional
public Order createOrder(OrderCreateRequest request) {
    // 1. Validate user and product
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Product product = productRepository.findById(request.getProductId())
        .orElseThrow(() -> new IllegalArgumentException("Product not found"));

    // 2. Check stock
    if (product.getStock() <= 0) {
        throw new IllegalArgumentException("Product is out of stock");
    }
    if (product.getStock() < request.getQuantity()) {
        throw new IllegalArgumentException("Insufficient stock");
    }

    // 3. Calculate total
    BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

    // 4. Create order
    Order order = new Order();
    // ... set fields ...

    // 5. Decrement stock
    product.setStock(product.getStock() - request.getQuantity());
    productRepository.save(product); // CRITICAL: Save updated stock

    // 6. Save order
    return orderRepository.save(order);
}
```

**Important:**
- MUST be @Transactional - if order save fails, stock rollback occurs
- Check stock BEFORE decrementing
- Use `productRepository.save(product)` to persist stock change
- Entire method is atomic: all succeed or all rollback
- In production, consider optimistic locking (@Version) for concurrent updates

### 4. Total Calculation

**CRITICAL:** Security and correctness.

**Problem:**
- If client sends total, malicious user can manipulate price
- Total must match product.price * quantity

**Solution:**
```java
// In OrderService.createOrder:
BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
order.setTotal(total);

// OrderCreateRequest has NO total field:
public class OrderCreateRequest {
    private Long userId;
    private Long productId;
    private Integer quantity;
    // NO total field - calculated server-side
}
```

**Important:**
- NEVER accept total from client
- Always calculate server-side: `product.price * quantity`
- Use `BigDecimal.valueOf(intValue)` for conversion
- If quantity updated, recalculate total automatically

### 5. Update Restrictions

**CRITICAL:** Business rule enforcement.

**Problem:**
- Completed or expired orders should be immutable
- Easy to forget status check

**Solution:**
```java
@Transactional
public Order updateOrder(Long id, OrderUpdateRequest request) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Order not found"));

    // Check status BEFORE updating
    if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.EXPIRED) {
        throw new IllegalArgumentException("Cannot update order with status: " + order.getStatus());
    }

    // Proceed with update...
}
```

**Important:**
- Check status at start of method
- Use enum comparison: `== OrderStatus.COMPLETED`
- Throw IllegalArgumentException to trigger 404 in controller
- Test both COMPLETED and EXPIRED status restrictions

### 6. Timestamps (@UpdateTimestamp is NEW)

**CRITICAL:** First use of @UpdateTimestamp in project.

**Problem:**
- Product entity only has @CreationTimestamp
- Easy to forget @UpdateTimestamp import

**Solution:**
```java
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@CreationTimestamp
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(nullable = false)
private LocalDateTime updatedAt;
```

**Important:**
- createdAt: `updatable = false` prevents modification
- updatedAt: NO `updatable = false` (needs to change)
- Both auto-managed by Hibernate
- DON'T set manually in service code
- Import from `org.hibernate.annotations.*`

### 7. BigDecimal Arithmetic

**CRITICAL:** Correct money handling.

**Problem:**
- `new BigDecimal(99.99)` causes precision errors
- BigDecimal multiplication requires special method

**Solution:**
```java
// Correct:
BigDecimal price = new BigDecimal("99.99");
BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

// Wrong:
BigDecimal price = new BigDecimal(99.99); // NEVER do this
BigDecimal total = price * quantity; // Won't compile
```

**Important:**
- Use string constructor: `new BigDecimal("99.99")`
- For multiplication: `.multiply(BigDecimal.valueOf(intValue))`
- BigDecimal is immutable - methods return new instance
- Use `isEqualByComparingTo()` in tests, not `equals()`

### 8. Testing Entity Relationships

**CRITICAL:** Repository tests need complete entity graphs.

**Problem:**
- Order requires User and Product to exist
- Can't create Order without foreign keys

**Solution:**
```java
@BeforeEach
void setUp() {
    // Must create User first
    testUser = new User();
    testUser.setName("Test User");
    testUser = userRepository.save(testUser);

    // Must create Product first
    testProduct = new Product();
    testProduct.setName("Test Product");
    testProduct = productRepository.save(testProduct);

    // Now can create Order
    testOrder = new Order();
    testOrder.setUser(testUser);
    testOrder.setProduct(testProduct);
    // ...
}
```

**Important:**
- Save User and Product BEFORE creating Order
- Use saved entities (with IDs) in Order
- @DataJpaTest provides all repositories automatically
- H2 enforces foreign key constraints in tests

### 9. Testing Complex Business Logic

**CRITICAL:** Service tests must cover all business rules.

**Problem:**
- Easy to forget edge cases
- Stock validation has multiple failure scenarios

**Solution - Test ALL scenarios:**
```java
@Test
void shouldThrowExceptionWhenStockInsufficient() { ... }

@Test
void shouldThrowExceptionWhenStockIsZero() { ... }

@Test
void shouldThrowExceptionWhenUpdatingCompletedOrder() { ... }

@Test
void shouldThrowExceptionWhenUpdatingExpiredOrder() { ... }
```

**Important:**
- Test stock validation failure
- Test status restriction (COMPLETED and EXPIRED)
- Test user/product not found
- Verify stock decrement: `assertThat(product.getStock()).isEqualTo(expected)`
- Use `assertThatThrownBy()` for exception testing

### 10. Controller Test Configuration

**CRITICAL:** JWT security configuration in tests.

**Problem:**
- JWT authentication will block all test requests
- Tests fail with 403 Forbidden without proper config

**Solution:**
```java
@WebMvcTest(value = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(TestSecurityConfig.class)
class OrderControllerTest {
    @MockBean
    private JwtService jwtService; // Required even if not used
    // ...
}
```

**Important:**
- MUST exclude JwtAuthenticationFilter
- MUST import TestSecurityConfig
- MUST mock JwtService (even if unused)
- Use `.with(csrf())` for POST/PUT/DELETE
- Follow ProductControllerTest pattern exactly

### 11. Database Schema Generation

**CRITICAL:** Hibernate auto-creates schema from entities.

**Important:**
- `spring.jpa.hibernate.ddl-auto: update` creates/updates tables
- Foreign keys auto-created from @JoinColumn
- Enum stored as VARCHAR with EnumType.STRING
- BigDecimal becomes DECIMAL(10,2) in PostgreSQL
- LocalDateTime becomes TIMESTAMP
- Verify with: `\d orders` in psql

### 12. API Endpoint Design

**Important:**
- 7 endpoints (not 5 like Product/User):
  1. GET /api/orders - all orders
  2. GET /api/orders/{id} - order by ID
  3. GET /api/orders/user/{userId} - orders by user
  4. GET /api/orders/status/{status} - orders by status
  5. POST /api/orders - create order
  6. PUT /api/orders/{id} - update order
  7. DELETE /api/orders/{id} - delete order
- Additional query endpoints for filtering
- All require JWT authentication

## Quality Checklist

- [ ] OrderStatus enum created (PENDING, PROCESSING, COMPLETED, EXPIRED)
- [ ] Order entity created with all required fields
- [ ] @ManyToOne relationships to User and Product
- [ ] @Enumerated(EnumType.STRING) for status
- [ ] @UpdateTimestamp for updated_at field
- [ ] OrderRepository created extending JpaRepository
- [ ] Custom query methods: findByUserId, findByStatus
- [ ] OrderCreateRequest DTO created with validation
- [ ] OrderUpdateRequest DTO created with optional fields
- [ ] OrderService created with all CRUD methods
- [ ] Stock validation before order creation
- [ ] Stock decrement in same @Transactional method
- [ ] Total auto-calculated (NOT from client)
- [ ] Update restriction when status is COMPLETED/EXPIRED
- [ ] OrderController created with 7 endpoints
- [ ] SecurityConfig updated to protect /api/orders/**
- [ ] OrderRepositoryTest created with @DataJpaTest
- [ ] OrderServiceTest created with Mockito
- [ ] Service tests cover stock validation scenarios
- [ ] Service tests cover update restriction scenarios
- [ ] OrderControllerTest created with @WebMvcTest
- [ ] All validation gates pass (compile, test, package)
- [ ] Application starts without errors
- [ ] orders table auto-created with foreign keys
- [ ] JWT authentication required for all order endpoints
- [ ] Stock correctly decremented after order creation
- [ ] Total correctly calculated as product.price * quantity
- [ ] Cannot update completed or expired orders
- [ ] Swagger UI shows Order Management section

## Confidence Score: 7/10

**Rationale:**

**Strong Foundation (+):**
- Existing Product and User modules provide pattern to follow
- JWT authentication already working
- All dependencies already in project
- Database already configured
- Clear requirements with exact field types

**Detailed Implementation (+):**
- Complete code snippets for every file
- Step-by-step tasks with exact file locations
- 24 tests planned (4 repo + 11 service + 9 controller)
- 18 executable validation gates
- Comprehensive error handling strategy

**Research Quality (+):**
- Documentation URLs for all new concepts (enums, relationships)
- Links to official docs for enum, @ManyToOne, @UpdateTimestamp
- Common pitfalls identified with solutions

**Risk Factors (-):**

1. **Enum Implementation (-1.0)**
   - First enum in project - no existing pattern
   - Easy to use EnumType.ORDINAL instead of STRING
   - Must remember @Enumerated annotation
   - Mitigation: Detailed gotcha #1, clear code example

2. **Entity Relationships (-1.0)**
   - First use of @ManyToOne in project
   - Complex @JoinColumn configuration
   - Foreign key constraints not obvious
   - Fetch type considerations
   - Mitigation: Detailed gotcha #2, complete code example

3. **Complex Transactional Logic (-0.5)**
   - Multiple repository saves in single transaction
   - Stock decrement must happen correctly
   - Race condition potential
   - Rollback behavior crucial
   - Mitigation: Detailed gotcha #3, @Transactional example

4. **@UpdateTimestamp NEW (-0.5)**
   - Not used anywhere in project yet
   - Different from @CreationTimestamp (no updatable=false)
   - Easy to forget import
   - Mitigation: Detailed gotcha #6, clear import statement

5. **Business Rules Enforcement (-0.3)**
   - Status-based update restriction needs careful testing
   - Stock validation has multiple edge cases
   - Easy to miss test scenarios
   - Mitigation: Comprehensive test examples, gotcha #9

**Strengths:**
- Pattern exists (Product module) but complexity higher
- All new concepts researched and documented
- Complete test coverage planned
- All validation gates executable
- Gotchas section covers all major pitfalls
- Quality checklist ensures nothing missed

**Expected Outcome:**
An AI agent with access to this PRP, the codebase, and web search should be able to implement the Orders module successfully, though with higher difficulty than the Products module due to:
- First enum implementation
- First entity relationships
- Complex transactional logic
- Multiple business rules

The score of 7/10 reflects that this is a more complex feature than Products (9/10) but still achievable with the comprehensive PRP guidance.

## Documentation Sources

This PRP was created using research from the following sources:

**Java Enums:**
- [Enum Types in Java - Oracle Official Tutorial](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html)
- [Using Enums with JPA - Baeldung](https://www.baeldung.com/jpa-persisting-enums-in-jpa)
- [EnumType.STRING vs EnumType.ORDINAL - Vlad Mihalcea](https://vladmihalcea.com/the-best-way-to-map-an-enum-type-with-jpa-and-hibernate/)

**JPA Relationships:**
- [@ManyToOne and @JoinColumn - Baeldung](https://www.baeldung.com/jpa-join-column)
- [JPA Entity Relationships - Spring Data JPA Docs](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Hibernate Many-to-One Mapping](https://www.baeldung.com/hibernate-one-to-many)

**Hibernate Timestamps:**
- [Hibernate @CreationTimestamp and @UpdateTimestamp | Baeldung](https://www.baeldung.com/hibernate-creationtimestamp-updatetimestamp)
- [Hibernate @CreationTimestamp Tutorial - JavaGuides](https://www.javaguides.net/2024/05/hibernate-creationtimestamp-and-updatetimestamp.html)

**Spring Transactions:**
- [Spring @Transactional - Baeldung](https://www.baeldung.com/transaction-configuration-with-jpa-and-spring)
- [Understanding Spring Transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

**BigDecimal:**
- [BigDecimal Best Practices - Baeldung](https://www.baeldung.com/java-bigdecimal-biginteger)

**Validation & Testing:**
- [Jakarta Bean Validation 3.0](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [@WebMvcTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)