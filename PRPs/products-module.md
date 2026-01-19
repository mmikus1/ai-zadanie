# PRP: Products Module with CRUD and JWT Authentication

## Context and Overview

This PRP defines the implementation of a complete Products module for the existing Spring Boot application. The authentication module (JWT) and user management module (CRUD) already exist, and we need to add a similar CRUD module for products with JWT-protected endpoints.

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database (already configured and running)
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction
- JWT authentication already implemented with Spring Security

**Feature Requirements:**
1. Create Product entity with fields: id, name, description, price, stock, created_at
2. Create ProductRepository extending JpaRepository
3. Create ProductService with business logic for CRUD operations
4. Create ProductController with 5 REST endpoints (GET all, GET by ID, POST create, PUT update, DELETE)
5. Create DTOs: ProductCreateRequest, ProductUpdateRequest
6. Protect all product endpoints with JWT authentication
7. Add comprehensive validation on request data
8. Create full test suite (controller, service, repository tests)

**Existing Codebase Context:**
- User entity exists at `src/main/java/com/example/zadanie/entity/User.java:1-37`
- UserController pattern to follow at `src/main/java/com/example/zadanie/api/controller/UserController.java:1-74`
- UserService pattern to follow at `src/main/java/com/example/zadanie/service/UserService.java:1-71`
- UserCreateRequest DTO pattern at `src/main/java/com/example/zadanie/api/dto/UserCreateRequest.java:1-28`
- UserUpdateRequest DTO pattern at `src/main/java/com/example/zadanie/api/dto/UserUpdateRequest.java:1-21`
- SecurityConfig with JWT filter at `src/main/java/com/example/zadanie/config/SecurityConfig.java:1-67`
- GlobalExceptionHandler at `src/main/java/com/example/zadanie/api/error/GlobalExceptionHandler.java:1-37`
- Test patterns at `src/test/java/com/example/zadanie/controller/AuthControllerTest.java:1-97` (shows JWT filter exclusion)

## Research Findings and Documentation

### Spring Data JPA and Entities

**Critical Resources:**
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/index.html)
- [Jakarta Persistence API 3.1](https://jakarta.ee/specifications/persistence/3.1/)
- [Hibernate ORM 6.6 Documentation](https://hibernate.org/orm/documentation/6.6/)
- [JPA Entity Design Best Practices - DEV Community](https://dev.to/protsenko/spring-data-jpa-best-practices-entity-design-guide-ad)

**Key Points:**
- Use `@Entity` and `@Table` annotations for JPA entities
- Use `@GeneratedValue(strategy = GenerationType.IDENTITY)` for auto-increment IDs
- Use `@Column` annotation to specify database constraints
- Jakarta EE (jakarta.persistence.*), NOT javax.persistence.*

### BigDecimal for Monetary Values

**Critical Resources:**
- [JPA Column Precision Scale Example](http://www.java2s.com/Tutorials/Java/JPA/0140__JPA_Column_Precision_Scale.htm)
- [Hibernate BigDecimal Precision Best Practices](https://www.baeldung.com/jpa-java-time)
- [Why Use BigDecimal for Money - Coderanch](https://coderanch.com/t/638935/databases/Hibernate-setting-decimal-points-BigDecimal)

**Key Points:**
- ALWAYS use `BigDecimal` for monetary values, NEVER float or double
- Use `@Column(precision = 10, scale = 2)` for price fields
- Precision = total number of digits, Scale = digits after decimal point
- Example: precision=10, scale=2 allows values like 99999999.99
- BigDecimal requires import from `java.math.BigDecimal`

### Hibernate Timestamp Annotations

**Critical Resources:**
- [Hibernate @CreationTimestamp and @UpdateTimestamp | Baeldung](https://www.baeldung.com/hibernate-creationtimestamp-updatetimestamp)
- [Hibernate @CreationTimestamp Tutorial - JavaGuides](https://www.javaguides.net/2024/05/hibernate-creationtimestamp-and-updatetimestamp.html)
- [Persist Creation and Update Timestamps - Thorben Janssen](https://thorben-janssen.com/persist-creation-update-timestamps-hibernate/)
- [Entity Create and Update Timestamps - HowToDoInJava](https://howtodoinjava.com/spring-data/entity-create-and-update-timestamps/)

**Key Points:**
- Use `@CreationTimestamp` for created_at field (auto-populated on INSERT)
- Use `@UpdateTimestamp` for updated_at field (auto-populated on UPDATE)
- Use `LocalDateTime` type (NOT Date or Timestamp)
- Add `@Column(updatable = false)` to created_at to prevent updates
- Import from `org.hibernate.annotations.CreationTimestamp`
- Import LocalDateTime from `java.time.LocalDateTime`

### Bean Validation

**Critical Resources:**
- [Jakarta Bean Validation 3.0](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Hibernate Validator](https://hibernate.org/validator/)
- [Spring Boot Validation Guide](https://spring.io/guides/gs/validating-form-input)

**Key Points:**
- Use `@NotNull` for required fields
- Use `@Size(max = 100)` for string length constraints
- Use `@DecimalMin("0.0")` for minimum decimal value
- Use `@Min(0)` for minimum integer value
- Use `@Valid` in controller to trigger validation
- Validation errors handled by GlobalExceptionHandler (already exists)

### Spring Boot REST with Spring Security

**Critical Resources:**
- [Spring Boot 3.5 Reference](https://docs.spring.io/spring-boot/reference/index.html)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Building REST Services with Spring](https://spring.io/guides/tutorials/rest)
- [Spring Security 6 Documentation](https://docs.spring.io/spring-security/reference/index.html)

**Key Points:**
- Product endpoints MUST be protected with JWT authentication
- Update SecurityConfig to add `.requestMatchers("/api/products/**").authenticated()`
- Controller tests need to exclude JwtAuthenticationFilter and use TestSecurityConfig
- Tests need `.with(csrf())` for POST/PUT/DELETE requests

### Testing with Spring Boot

**Critical Resources:**
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [@WebMvcTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)
- [@DataJpaTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)

**Key Points:**
- Use `@WebMvcTest` for controller tests with MockMvc
- Use `@DataJpaTest` for repository tests with H2 database
- Use `@ExtendWith(MockitoExtension.class)` for service unit tests
- Follow AuthControllerTest pattern for JWT security configuration in tests

## Implementation Blueprint

### High-Level Approach

```
1. Entity Layer
   ├── Create Product entity with all required fields
   ├── Use BigDecimal for price
   ├── Use Integer for stock
   ├── Use LocalDateTime with @CreationTimestamp for created_at
   └── Add field-level validation annotations

2. Repository Layer
   ├── Create ProductRepository interface
   └── Extend JpaRepository<Product, Long>

3. DTO Layer
   ├── Create ProductCreateRequest with all required fields
   ├── Create ProductUpdateRequest with optional fields
   └── Add validation annotations to DTOs

4. Service Layer
   ├── Create ProductService with @Service annotation
   ├── Implement getAllProducts(), getProductById(), createProduct(), updateProduct(), deleteProduct()
   ├── Add @Transactional for write operations
   └── Add business validation (e.g., prevent negative prices, check duplicates)

5. Controller Layer
   ├── Create ProductController with @RestController
   ├── Implement 5 CRUD endpoints
   ├── Add @Valid for request validation
   ├── Add OpenAPI/Swagger annotations
   └── Handle exceptions with try-catch

6. Security Configuration
   ├── Update SecurityConfig.java
   └── Add .requestMatchers("/api/products/**").authenticated()

7. Testing
   ├── Create ProductRepositoryTest with @DataJpaTest
   ├── Create ProductServiceTest with Mockito
   ├── Create ProductControllerTest with @WebMvcTest
   └── Follow AuthControllerTest pattern for security setup

8. Validation & Integration
   ├── Compile and run tests
   ├── Test with curl commands
   └── Verify Swagger UI integration
```

### Package Structure

```
com.example.zadanie/
├── entity/
│   └── Product.java (NEW)
├── repository/
│   └── ProductRepository.java (NEW)
├── service/
│   └── ProductService.java (NEW)
├── api/
│   ├── controller/
│   │   └── ProductController.java (NEW)
│   ├── dto/
│   │   ├── ProductCreateRequest.java (NEW)
│   │   └── ProductUpdateRequest.java (NEW)
│   └── error/
│       └── GlobalExceptionHandler.java (EXISTING - no changes)
├── config/
│   └── SecurityConfig.java (UPDATE - add product endpoints)
└── test/
    ├── entity/ (optional)
    ├── repository/
    │   └── ProductRepositoryTest.java (NEW)
    ├── service/
    │   └── ProductServiceTest.java (NEW)
    ├── controller/
    │   └── ProductControllerTest.java (NEW)
    └── config/
        └── TestSecurityConfig.java (EXISTING - reuse)
```

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Create Product Entity

Create `src/main/java/com/example/zadanie/entity/Product.java`:

```java
package com.example.zadanie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    @Column(nullable = false)
    private Integer stock;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

**Key Points:**
- Entity name: `Product` (singular), table name: `products` (plural)
- Uses Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` matching User entity pattern
- BigDecimal for price with precision=10, scale=2 (allows values up to 99999999.99)
- Integer for stock (could use Long if large quantities expected)
- LocalDateTime for createdAt with @CreationTimestamp (auto-populated)
- Field-level validation with @NotNull, @Size, @DecimalMin, @Min
- description uses TEXT column type for longer content

### Task 2: Create Product Repository

Create `src/main/java/com/example/zadanie/repository/ProductRepository.java`:

```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // JpaRepository provides:
    // - save(Product product)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - existsById(Long id)

    // Custom query method (optional, for future use)
    Optional<Product> findByName(String name);
}
```

**Key Points:**
- Extends `JpaRepository<Product, Long>` (entity type, ID type)
- Basic CRUD methods provided automatically by Spring Data JPA
- Optional custom query method `findByName()` for future duplicate checking
- `@Repository` annotation (optional but recommended for clarity)

### Task 3: Create ProductCreateRequest DTO

Create `src/main/java/com/example/zadanie/api/dto/ProductCreateRequest.java`:

```java
package com.example.zadanie.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotNull(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    private Integer stock;
}
```

**Key Points:**
- All fields required for product creation (except description which is optional)
- Same validation as entity but only for creation fields
- No id or createdAt (auto-generated)
- Follows UserCreateRequest pattern

### Task 4: Create ProductUpdateRequest DTO

Create `src/main/java/com/example/zadanie/api/dto/ProductUpdateRequest.java`:

```java
package com.example.zadanie.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    private Integer stock;
}
```

**Key Points:**
- All fields optional (partial update)
- No @NotNull annotations (allows updating specific fields only)
- Still has validation constraints (if provided, must be valid)
- Follows UserUpdateRequest pattern

### Task 5: Create ProductService

Create `src/main/java/com/example/zadanie/service/ProductService.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.entity.Product;
import com.example.zadanie.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        // createdAt is auto-populated by @CreationTimestamp

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        // Only update fields that are provided (not null)
        if (request.getName() != null) {
            product.setName(request.getName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }

        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
```

**Key Points:**
- `@Service` for Spring service bean
- `@RequiredArgsConstructor` for constructor injection
- `@Transactional` on write operations (create, update, delete)
- Returns `Optional<Product>` for getById (null-safe)
- Throws `IllegalArgumentException` for not found cases (caught by controller)
- Partial update in updateProduct (only updates provided fields)
- Follows UserService pattern exactly

### Task 6: Create ProductController

Create `src/main/java/com/example/zadanie/api/controller/ProductController.java`:

```java
package com.example.zadanie.api.controller;

import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.entity.Product;
import com.example.zadanie.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves a list of all products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a specific product by ID")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        try {
            Product product = productService.createProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        try {
            Product product = productService.updateProduct(id, request);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by ID")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Key Points:**
- `@RestController` and `@RequestMapping("/api/products")`
- `@RequiredArgsConstructor` for dependency injection
- OpenAPI annotations: `@Tag` for controller, `@Operation` for each endpoint
- `@Valid` on POST and PUT to trigger validation
- Returns appropriate HTTP status codes:
  - 200 OK for GET, PUT
  - 201 CREATED for POST
  - 204 NO_CONTENT for DELETE
  - 404 NOT_FOUND when product doesn't exist
  - 400 BAD_REQUEST for validation errors (auto-handled by GlobalExceptionHandler)
- Follows UserController pattern exactly

### Task 7: Update SecurityConfig to Protect Product Endpoints

Update `src/main/java/com/example/zadanie/config/SecurityConfig.java`:

Find the `authorizeHttpRequests` section (around line 37-41) and add product endpoints:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .requestMatchers("/api/users/**").authenticated()
        .requestMatchers("/api/products/**").authenticated()  // ADD THIS LINE
        .anyRequest().authenticated()
)
```

**Location:** Add after line 40 in `SecurityConfig.java`

**Key Points:**
- All product endpoints require JWT authentication
- Unauthenticated requests will get 401 UNAUTHORIZED (handled by Spring Security)
- JWT token must be sent in Authorization header: `Bearer {token}`

### Task 8: Create ProductRepositoryTest

Create `src/test/java/com/example/zadanie/repository/ProductRepositoryTest.java`:

```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProduct() {
        // Given
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(10);

        // When
        Product saved = productRepository.save(product);
        Optional<Product> found = productRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(found.get().getStock()).isEqualTo(10);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindProductByName() {
        // Given
        Product product = new Product();
        product.setName("Unique Product");
        product.setDescription("Description");
        product.setPrice(new BigDecimal("49.99"));
        product.setStock(5);
        productRepository.save(product);

        // When
        Optional<Product> found = productRepository.findByName("Unique Product");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Unique Product");
    }

    @Test
    void shouldReturnEmptyWhenProductNotFound() {
        // When
        Optional<Product> found = productRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }
}
```

**Key Points:**
- `@DataJpaTest` for repository testing with H2 in-memory database
- Tests basic CRUD operations
- Tests custom findByName query method
- Uses BigDecimal comparison with `isEqualByComparingTo()`
- Verifies createdAt is auto-populated

### Task 9: Create ProductServiceTest

Create `src/test/java/com/example/zadanie/service/ProductServiceTest.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.entity.Product;
import com.example.zadanie.repository.ProductRepository;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
    }

    @Test
    void shouldGetAllProducts() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

        // When
        List<Product> products = productService.getAllProducts();

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Test Product");
        verify(productRepository).findAll();
    }

    @Test
    void shouldGetProductById() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        Optional<Product> found = productService.getProductById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        verify(productRepository).findById(1L);
    }

    @Test
    void shouldCreateProduct() {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
            "New Product", "Description", new BigDecimal("49.99"), 5);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product created = productService.createProduct(request);

        // Then
        assertThat(created).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldUpdateProduct() {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", "Updated Description", new BigDecimal("79.99"), 20);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product updated = productService.updateProduct(1L, request);

        // Then
        assertThat(updated).isNotNull();
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", null, null, null);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> productService.updateProduct(999L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void shouldDeleteProduct() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> productService.deleteProduct(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product not found");
    }
}
```

**Key Points:**
- `@ExtendWith(MockitoExtension.class)` for Mockito support
- `@Mock` for repository dependency
- `@InjectMocks` for service under test
- Tests all service methods
- Tests exception handling
- Uses `verify()` to check repository method calls

### Task 10: Create ProductControllerTest

Create `src/test/java/com/example/zadanie/controller/ProductControllerTest.java`:

```java
package com.example.zadanie.controller;

import com.example.zadanie.api.controller.ProductController;
import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.config.JwtAuthenticationFilter;
import com.example.zadanie.config.TestSecurityConfig;
import com.example.zadanie.entity.Product;
import com.example.zadanie.service.JwtService;
import com.example.zadanie.service.ProductService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
    }

    @Test
    void shouldGetAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(Arrays.asList(testProduct));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Test Product"))
            .andExpect(jsonPath("$[0].price").value(99.99))
            .andExpect(jsonPath("$[0].stock").value(10));
    }

    @Test
    void shouldGetProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Product"))
            .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "New Product", "Description", new BigDecimal("49.99"), 5);

        when(productService.createProduct(any())).thenReturn(testProduct);

        mockMvc.perform(post("/api/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void shouldReturnBadRequestForInvalidProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "", null, new BigDecimal("-10"), -5);  // Invalid data

        mockMvc.perform(post("/api/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", "Updated Description", new BigDecimal("79.99"), 20);

        when(productService.updateProduct(eq(1L), any())).thenReturn(testProduct);

        mockMvc.perform(put("/api/products/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentProduct() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", null, null, null);

        when(productService.updateProduct(eq(999L), any()))
            .thenThrow(new IllegalArgumentException("Product not found"));

        mockMvc.perform(put("/api/products/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentProduct() throws Exception {
        when(productService.deleteProduct(999L))
            .thenThrow(new IllegalArgumentException("Product not found"));

        // Note: deleteProduct returns void, so we need to use doThrow
        mockMvc.perform(delete("/api/products/999")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }
}
```

**Key Points:**
- `@WebMvcTest(ProductController.class)` for controller testing
- Excludes `JwtAuthenticationFilter` to avoid JWT token validation in tests
- Imports `TestSecurityConfig` for test security configuration
- `@MockBean` for ProductService and JwtService
- Uses `.with(csrf())` for POST/PUT/DELETE requests
- Tests all HTTP methods and error scenarios
- Uses `ObjectMapper` to serialize request bodies
- Follows AuthControllerTest pattern exactly

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Clean and compile the project
./mvnw clean compile

# Expected output: BUILD SUCCESS

# 2. Run all tests
./mvnw test

# Expected output: All tests pass (including new Product tests)
# Expected: 20+ existing tests + 3 ProductRepositoryTest + 7 ProductServiceTest + 8 ProductControllerTest = 38+ tests

# 3. Package the application
./mvnw package

# Expected output: BUILD SUCCESS, JAR created in target/

# 4. Start the application (ensure PostgreSQL is running)
docker compose up -d  # Start PostgreSQL if not already running
./mvnw spring-boot:run

# Expected output: Application starts on port 8080
# Expected: Product table auto-created in database

# 5. Get JWT token for testing
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"Test@1234"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with JWT token (save this token for next steps)
# Example: {"token":"eyJhbGciOiJIUzI1NiJ9...","type":"Bearer","email":"testuser@example.com","userId":1}
# Export token: export TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 6. Test Create Product (with JWT token)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Test Product","description":"A test product","price":"99.99","stock":10}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 201 CREATED with product JSON including id and createdAt
# Example: {"id":1,"name":"Test Product","description":"A test product","price":99.99,"stock":10,"createdAt":"2026-01-18T..."}

# 7. Test Get All Products (with JWT token)
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with array of products

# 8. Test Get Product by ID (with JWT token)
curl -X GET http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with product JSON

# 9. Test Update Product (with JWT token)
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Updated Product","price":"79.99","stock":20}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with updated product JSON

# 10. Test Delete Product (with JWT token)
curl -X DELETE http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nStatus: %{http_code}\n"

# Expected output: 204 NO CONTENT

# 11. Test Unauthorized Access (without JWT token)
curl -X GET http://localhost:8080/api/products \
  -w "\nStatus: %{http_code}\n"

# Expected output: 401 UNAUTHORIZED

# 12. Test Invalid Product Creation (with JWT token, but invalid data)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"","price":"-10","stock":-5}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 400 BAD REQUEST with validation errors
# Example: {"name":"Name is required","price":"Price must be greater than or equal to 0","stock":"Stock must be greater than or equal to 0"}

# 13. Verify Swagger UI
# Open browser: http://localhost:8080/swagger-ui.html
# Expected: See "Product Management" section with 5 endpoints
# Expected: GET /api/products, GET /api/products/{id}, POST /api/products, PUT /api/products/{id}, DELETE /api/products/{id}

# 14. Verify database table created
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "\d products"

# Expected output: Table structure showing columns: id, name, description, price, stock, created_at

# 15. Query products from database
docker compose exec postgres psql -U zadanie_user -d zadanie_db -c "SELECT * FROM products;"

# Expected output: Product records with prices showing 2 decimal places, created_at timestamps
```

## Error Handling Strategy

### Validation Errors (400 Bad Request)

**Scenario 1: Missing Required Fields**
- Triggered by: `@NotNull` annotations in DTOs
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"name": "Name is required", "price": "Price is required", "stock": "Stock is required"}`

**Scenario 2: Invalid Field Values**
- Triggered by: `@DecimalMin`, `@Min`, `@Size` annotations
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"price": "Price must be greater than or equal to 0", "stock": "Stock must be greater than or equal to 0"}`

**Scenario 3: Invalid String Length**
- Triggered by: `@Size(max = 100)` on name field
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"name": "Name must not exceed 100 characters"}`

### Resource Not Found (404 Not Found)

**Scenario 4: Product Not Found (GET by ID)**
- Service returns `Optional.empty()`
- Controller returns `ResponseEntity.notFound().build()`
- HTTP Status: 404
- Response Body: empty

**Scenario 5: Product Not Found (UPDATE)**
- Service throws `IllegalArgumentException("Product not found with id: X")`
- Controller catches exception and returns `ResponseEntity.notFound().build()`
- HTTP Status: 404
- Response Body: empty

**Scenario 6: Product Not Found (DELETE)**
- Service throws `IllegalArgumentException("Product not found with id: X")`
- Controller catches exception and returns `ResponseEntity.notFound().build()`
- HTTP Status: 404
- Response Body: empty

### Authentication Errors (401 Unauthorized)

**Scenario 7: Missing JWT Token**
- Spring Security JwtAuthenticationFilter intercepts request
- No token in Authorization header
- HTTP Status: 401
- Response Body: Spring Security default error response

**Scenario 8: Invalid JWT Token**
- Spring Security JwtAuthenticationFilter validates token
- Token is expired, malformed, or has invalid signature
- HTTP Status: 401
- Response Body: Spring Security default error response

## Gotchas and Critical Considerations

### 1. BigDecimal Precision and Scale

**CRITICAL:** Use BigDecimal for monetary values, NEVER float or double.

**Problem:**
- Float and double have precision issues: `0.1 + 0.2 != 0.3` in floating point
- Unacceptable for financial calculations

**Solution:**
```java
@Column(precision = 10, scale = 2)
private BigDecimal price;
```

**Important:**
- precision = total digits (including decimal)
- scale = digits after decimal point
- precision=10, scale=2 allows: -99999999.99 to 99999999.99
- Always use `new BigDecimal("99.99")` (string constructor), NOT `new BigDecimal(99.99)` (double constructor)

**In Tests:**
- Use `isEqualByComparingTo()` for BigDecimal assertions
- Example: `assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));`

### 2. Timestamp Auto-Population

**CRITICAL:** Use @CreationTimestamp, don't set manually.

**Problem:**
- Manually setting createdAt in service layer is error-prone
- Inconsistent timestamps across application

**Solution:**
```java
@CreationTimestamp
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;
```

**Important:**
- `@CreationTimestamp` auto-populates on INSERT
- `updatable = false` prevents changes on UPDATE
- Import from `org.hibernate.annotations.CreationTimestamp`
- DON'T call `product.setCreatedAt()` in service layer

**Optional Enhancement:**
- Add `updatedAt` field with `@UpdateTimestamp` for tracking updates

### 3. JWT Authentication in Tests

**CRITICAL:** Exclude JwtAuthenticationFilter in controller tests.

**Problem:**
- JWT filter validates tokens in all requests
- Tests will fail with 401 unless token is provided
- MockMvc tests don't easily support JWT token generation

**Solution:**
```java
@WebMvcTest(value = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(TestSecurityConfig.class)
class ProductControllerTest {
    // ...
}
```

**Important:**
- Always exclude JwtAuthenticationFilter in @WebMvcTest
- Import TestSecurityConfig (already exists in project)
- Use `.with(csrf())` for POST/PUT/DELETE requests
- Mock JwtService with `@MockBean` even if not used

### 4. Security Configuration Update

**CRITICAL:** Add product endpoints to SecurityConfig.

**Problem:**
- If you forget to update SecurityConfig, product endpoints will deny all requests
- Spring Security default is "deny all" unless explicitly permitted

**Solution:**
Add this line after line 40 in SecurityConfig.java:
```java
.requestMatchers("/api/products/**").authenticated()
```

**Important:**
- Must be added BEFORE `.anyRequest().authenticated()`
- Order matters in Spring Security configuration
- Test both authenticated and unauthenticated requests

### 5. Partial Updates in PUT

**Implementation:**
- ProductUpdateRequest has all fields optional
- Service checks each field before updating

**Important:**
- Only provided fields (not null) are updated
- Allows updating name without changing price/stock
- Follows REST best practices for PUT with partial updates

**Alternative:**
- PATCH method for partial updates
- PUT method for full replacement
- Current implementation treats PUT as partial update

### 6. Entity vs DTO Validation

**Important:**
- Validation annotations in BOTH entity and DTO
- Entity validation: database layer protection
- DTO validation: API layer protection (better error messages)
- Don't rely on only one layer

**Why Both:**
- DTO validation gives clear API error messages
- Entity validation prevents invalid data in database
- Defense in depth

### 7. Repository Method Return Types

**Important:**
- `findById()` returns `Optional<Product>` (null-safe)
- `save()` returns Product (never null)
- `findAll()` returns List (empty if no results, never null)

**In Service:**
```java
// Good - null-safe
Optional<Product> product = productRepository.findById(id);

// Bad - will throw NullPointerException if not found
Product product = productRepository.findById(id).get();
```

### 8. Transaction Management

**Important:**
- `@Transactional` on service methods that modify data
- NOT on controller methods
- Repository methods are transactional by default

**Why:**
- Service layer is correct place for transaction boundaries
- Multiple repository calls in one transaction
- Rollback on exception

### 9. OpenAPI/Swagger Integration

**Already Configured:**
- Springdoc OpenAPI is in pom.xml
- Swagger UI accessible at http://localhost:8080/swagger-ui.html

**Important:**
- Add `@Tag` to controller for grouping
- Add `@Operation` to each endpoint for documentation
- Swagger UI auto-generates from annotations
- No additional configuration needed

### 10. PostgreSQL Auto-Create Table

**Important:**
- Hibernate `ddl-auto: update` in application.yaml
- Product table auto-created on application startup
- Columns match entity definition
- BigDecimal becomes DECIMAL(10,2) in database
- LocalDateTime becomes TIMESTAMP in database

**Database:**
- Table name: `products` (from @Table annotation)
- Auto-increment ID (SERIAL in PostgreSQL)
- NOT NULL constraints from @Column(nullable = false)

## Quality Checklist

- [ ] Product entity created with all required fields
- [ ] ProductRepository created extending JpaRepository
- [ ] ProductCreateRequest DTO created with validation
- [ ] ProductUpdateRequest DTO created with optional fields
- [ ] ProductService created with all CRUD methods
- [ ] ProductController created with 5 endpoints
- [ ] SecurityConfig updated to protect /api/products/**
- [ ] ProductRepositoryTest created with @DataJpaTest
- [ ] ProductServiceTest created with Mockito
- [ ] ProductControllerTest created with @WebMvcTest
- [ ] All validation gates pass (compile, test, package)
- [ ] Application starts without errors
- [ ] Product table auto-created in database
- [ ] JWT authentication required for all product endpoints
- [ ] Unauthenticated requests return 401
- [ ] Invalid data returns 400 with validation errors
- [ ] Not found returns 404
- [ ] Swagger UI shows Product Management section
- [ ] BigDecimal used for price (not float/double)
- [ ] @CreationTimestamp used for createdAt
- [ ] Tests exclude JwtAuthenticationFilter

## Confidence Score: 9/10

**Rationale:**

**Strong Foundation (+):**
- Existing User module provides complete pattern to follow
- JWT authentication already implemented and tested
- Clear, specific requirements with exact field types
- Comprehensive codebase references with line numbers
- All dependencies already in project (no new libraries needed)
- Database already configured and running

**Detailed Implementation (+):**
- Complete code snippets for every file (entity, repository, service, controller, DTOs, tests)
- Step-by-step tasks with exact file locations
- Multiple test classes covering all scenarios (38+ tests total)
- Error handling strategy clearly defined
- 15 executable validation gates with expected outputs

**Research Quality (+):**
- BigDecimal best practices researched and documented
- Hibernate timestamp annotations researched
- Links to official documentation for all technologies
- Common pitfalls identified and solutions provided

**Risk Factors (-):**

1. **BigDecimal String Constructor (-0.5)**
   - Easy to accidentally use `new BigDecimal(99.99)` instead of `new BigDecimal("99.99")`
   - Former has precision issues
   - Mitigation: Clearly documented in gotchas, test assertions show correct usage

2. **Security Configuration Update (-0.5)**
   - Easy to forget updating SecurityConfig
   - All endpoints would fail without this change
   - Mitigation: Explicit task (Task 7) with exact line number and code, validation gate #11 tests unauthorized access

**Strengths:**
- Pattern-matching implementation (mirrors existing User module exactly)
- No new concepts to learn (JWT, JPA, validation all already used in project)
- Complete test coverage planned
- All validation gates are executable and specific
- Gotchas section covers all major pitfalls
- Quality checklist ensures nothing is missed

**Expected Outcome:**
An AI agent with access to this PRP, the codebase, and web search should be able to implement the Products module successfully in one pass, with clear validation gates to confirm success. The only minor risks are configuration mistakes that are explicitly documented and have validation tests.

## Documentation Sources

This PRP was created using research from the following sources:

**Spring Data JPA:**
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/index.html)
- [Jakarta Persistence API 3.1](https://jakarta.ee/specifications/persistence/3.1/)
- [Hibernate ORM 6.6 Documentation](https://hibernate.org/orm/documentation/6.6/)
- [Spring Data JPA Best Practices - DEV Community](https://dev.to/protsenko/spring-data-jpa-best-practices-entity-design-guide-ad)

**BigDecimal for Monetary Values:**
- [JPA Column Precision Scale Example](http://www.java2s.com/Tutorials/Java/JPA/0140__JPA_Column_Precision_Scale.htm)
- [BigDecimal Hibernate Best Practices - Coderanch](https://coderanch.com/t/638935/databases/Hibernate-setting-decimal-points-BigDecimal)

**Hibernate Timestamps:**
- [Hibernate @CreationTimestamp and @UpdateTimestamp | Baeldung](https://www.baeldung.com/hibernate-creationtimestamp-updatetimestamp)
- [Hibernate Timestamp Tutorial - JavaGuides](https://www.javaguides.net/2024/05/hibernate-creationtimestamp-and-updatetimestamp.html)
- [Persist Creation and Update Timestamps - Thorben Janssen](https://thorben-janssen.com/persist-creation-update-timestamps-hibernate/)
- [Entity Timestamps - HowToDoInJava](https://howtodoinjava.com/spring-data/entity-create-and-update-timestamps/)

**Validation:**
- [Jakarta Bean Validation 3.0](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Hibernate Validator](https://hibernate.org/validator/)

**Spring Boot & REST:**
- [Spring Boot 3.5 Reference](https://docs.spring.io/spring-boot/reference/index.html)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Building REST Services with Spring](https://spring.io/guides/tutorials/rest)
- [Spring Security 6 Documentation](https://docs.spring.io/spring-security/reference/index.html)

**Testing:**
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [@WebMvcTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)
- [@DataJpaTest Documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)
