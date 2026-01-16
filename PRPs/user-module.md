# PRP: User Module with CRUD Operations

## Context and Overview

This PRP defines the implementation of a complete user management module for the Spring Boot application. This will be the **first module** in the codebase - no entities, repositories, or controllers exist yet.

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction

**Feature Requirements:**
1. User entity with JPA mapping (id, name, email, password)
2. CRUD REST API endpoints
3. Input validation (email format, password strength) with 400 responses
4. Docker Compose setup for PostgreSQL
5. Swagger/OpenAPI documentation
6. Comprehensive tests

## Research Findings and Documentation

### SpringDoc OpenAPI for Spring Boot 3.5
**Version to use:** 2.8.15 (latest stable, compatible with Spring Boot 3.5.9)

**Critical:** Spring Boot 3.5.x requires springdoc-openapi 2.8.9+ due to Spring Framework 6.2.x API changes. Earlier versions (2.5.x) cause `NoSuchMethodError` with ControllerAdviceBean.

**Resources:**
- [SpringDoc OpenAPI GitHub](https://github.com/springdoc/springdoc-openapi)
- [SpringDoc OpenAPI Maven Central](https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui)
- [Spring Boot 3.5 Compatibility Issue #3041](https://github.com/springdoc/springdoc-openapi/issues/3041)
- [Baeldung OpenAPI 3.0 Guide](https://www.baeldung.com/spring-rest-openapi-documentation)

### Jakarta Bean Validation
- [Spring Boot Validation Reference](https://docs.spring.io/spring-boot/reference/io/validation.html)
- [Custom Validation Guide](https://www.bezkoder.com/spring-boot-custom-validation/)
- [Jakarta Validation in Spring](https://agussyahrilmubarok.medium.com/guide-to-field-validation-with-jakarta-validation-in-spring-8c9eca68022e)
- [Custom Password Validator Examples](https://gist.github.com/aoudiamoncef/9eeece142d1ef0faa4d06216a41282a2)

### Spring Boot 3.5.9 Specific Documentation
- [Spring Data JPA Reference](https://docs.spring.io/spring-boot/3.5.9/reference/data/sql.html#data.sql.jpa-and-spring-data)
- [Spring Web Reference](https://docs.spring.io/spring-boot/3.5.9/reference/web/servlet.html)
- [Accessing Data with JPA Guide](https://spring.io/guides/gs/accessing-data-jpa/)
- [Building REST Services Guide](https://spring.io/guides/gs/rest-service/)

### Docker and PostgreSQL
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)

### Testing
- JUnit 5 with `@SpringBootTest` for integration tests
- Consider H2 for test database or Testcontainers for PostgreSQL

## Implementation Blueprint

### High-Level Approach

```
1. Infrastructure Setup
   ├── Add SpringDoc OpenAPI dependency to pom.xml
   ├── Add Bean Validation dependency
   ├── Create docker-compose.yml with PostgreSQL
   └── Configure application.yaml with database connection

2. Data Layer (JPA)
   ├── Create User entity with Lombok annotations
   ├── Define JPA mappings and constraints
   └── Create UserRepository interface

3. Validation Layer
   ├── Create custom @StrongPassword annotation
   ├── Implement PasswordValidator with regex
   └── Add validation annotations to entity/DTOs

4. Service Layer (Optional but Recommended)
   └── Create UserService for business logic separation

5. API Layer (REST)
   ├── Create UserController with CRUD endpoints
   ├── Implement proper HTTP status codes
   ├── Add validation with @Valid
   └── Add OpenAPI annotations for documentation

6. Testing
   ├── Create repository tests
   ├── Create controller integration tests
   └── Test validation scenarios

7. Validation & Documentation
   ├── Verify compilation
   ├── Test all endpoints
   └── Verify Swagger UI accessibility
```

### Package Structure
```
com.example.zadanie/
├── entity/
│   └── User.java
├── repository/
│   └── UserRepository.java
├── service/
│   └── UserService.java
├── controller/
│   └── UserController.java
├── dto/ (optional)
│   ├── UserCreateRequest.java
│   └── UserUpdateRequest.java
└── validation/
    ├── StrongPassword.java (annotation)
    └── StrongPasswordValidator.java
```

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Add Dependencies to pom.xml
Add the following dependencies:
```xml
<!-- SpringDoc OpenAPI for Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.15</version>
</dependency>

<!-- Bean Validation (already transitively included, but explicit is better) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- H2 for testing (optional) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**Location:** Add after line 56 in `pom.xml`

### Task 2: Create Docker Compose File
Create `docker-compose.yml` in project root:
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

volumes:
  postgres_data:
```

### Task 3: Configure Database in application.yaml
Update `src/main/resources/application.yaml`:
```yaml
spring:
  application:
    name: zadanie
  datasource:
    url: jdbc:postgresql://localhost:5432/zadanie_db
    username: zadanie_user
    password: zadanie_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

# SpringDoc OpenAPI Configuration
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

### Task 4: Create User Entity
Create `src/main/java/com/example/zadanie/entity/User.java`:
```java
package com.example.zadanie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotNull(message = "Password is required")
    @Column(nullable = false)
    private String password;
}
```

### Task 5: Create Custom Password Validator
Create `src/main/java/com/example/zadanie/validation/StrongPassword.java`:
```java
package com.example.zadanie.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
@Documented
public @interface StrongPassword {
    String message() default "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

Create `src/main/java/com/example/zadanie/validation/StrongPasswordValidator.java`:
```java
package com.example.zadanie.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final String PASSWORD_PATTERN =
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return password.matches(PASSWORD_PATTERN);
    }
}
```

### Task 6: Create DTOs (Recommended for separation)
Create `src/main/java/com/example/zadanie/dto/UserCreateRequest.java`:
```java
package com.example.zadanie.dto;

import com.example.zadanie.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotNull(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotNull(message = "Password is required")
    @StrongPassword
    private String password;
}
```

Create `src/main/java/com/example/zadanie/dto/UserUpdateRequest.java`:
```java
package com.example.zadanie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
}
```

### Task 7: Create Repository
Create `src/main/java/com/example/zadanie/repository/UserRepository.java`:
```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### Task 8: Create Service Layer
Create `src/main/java/com/example/zadanie/service/UserService.java`:
```java
package com.example.zadanie.service;

import com.example.zadanie.dto.UserCreateRequest;
import com.example.zadanie.dto.UserUpdateRequest;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // TODO: Hash password if security is needed

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
```

### Task 9: Create REST Controller
Create `src/main/java/com/example/zadanie/controller/UserController.java`:
```java
package com.example.zadanie.controller;

import com.example.zadanie.dto.UserCreateRequest;
import com.example.zadanie.dto.UserUpdateRequest;
import com.example.zadanie.entity.User;
import com.example.zadanie.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user profiles")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all user profiles")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user profile by ID")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user profile")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateRequest request) {
        try {
            User user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user profile")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            User user = userService.updateUser(id, request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user profile by ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

### Task 10: Create Exception Handler (Better Error Responses)
Create `src/main/java/com/example/zadanie/controller/GlobalExceptionHandler.java`:
```java
package com.example.zadanie.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
```

### Task 11: Create Tests
Create `src/test/java/com/example/zadanie/repository/UserRepositoryTest.java`:
```java
package com.example.zadanie.repository;

import com.example.zadanie.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        User user = new User(null, "John Doe", "john@example.com", "password123");
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldFindUserByEmail() {
        User user = new User(null, "Jane Doe", "jane@example.com", "password123");
        userRepository.save(user);

        assertThat(userRepository.findByEmail("jane@example.com")).isPresent();
    }

    @Test
    void shouldCheckEmailExists() {
        User user = new User(null, "Test User", "test@example.com", "password123");
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
}
```

Create `src/test/java/com/example/zadanie/controller/UserControllerTest.java`:
```java
package com.example.zadanie.controller;

import com.example.zadanie.dto.UserCreateRequest;
import com.example.zadanie.entity.User;
import com.example.zadanie.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void shouldGetAllUsers() throws Exception {
        User user = new User(1L, "John Doe", "john@example.com", "password");
        when(userService.getAllUsers()).thenReturn(Arrays.asList(user));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void shouldCreateUserWithValidData() throws Exception {
        UserCreateRequest request = new UserCreateRequest("John Doe", "john@example.com", "Pass@123");
        User user = new User(1L, "John Doe", "john@example.com", "Pass@123");

        when(userService.createUser(any())).thenReturn(user);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBadRequestForInvalidEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest("John Doe", "invalid-email", "Pass@123");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

### Task 12: Update User Entity with @StrongPassword
After creating the validation, update the User entity to use it:
```java
// In User.java, change password field to:
@NotNull(message = "Password is required")
@StrongPassword
@Column(nullable = false)
private String password;
```

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Ensure PostgreSQL is running
docker-compose up -d
docker-compose ps

# 2. Compile the project (checks syntax and dependencies)
./mvnw clean compile

# 3. Run all tests
./mvnw test

# 4. Package the application
./mvnw package

# 5. Run the application
./mvnw spring-boot:run

# 6. Manual API Testing (in another terminal)
# Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"Test@123"}'

# Get all users
curl http://localhost:8080/api/users

# Get user by ID
curl http://localhost:8080/api/users/1

# Update user
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated Name"}'

# Delete user
curl -X DELETE http://localhost:8080/api/users/1

# Test validation - should return 400
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"invalid-email","password":"weak"}' \
  -w "\nStatus: %{http_code}\n"

# 7. Verify Swagger UI
# Open browser: http://localhost:8080/swagger-ui.html
# Should see User Management APIs documented

# 8. Check OpenAPI JSON
curl http://localhost:8080/api-docs
```

## Error Handling Strategy

### Validation Errors (400 Bad Request)
- Triggered automatically by `@Valid` annotation
- `GlobalExceptionHandler` catches `MethodArgumentNotValidException`
- Returns JSON map of field errors: `{"field": "error message"}`

### Not Found (404)
- Repository returns `Optional.empty()`
- Controller maps to `ResponseEntity.notFound()`

### Duplicate Email (400 Bad Request)
- Service throws `IllegalArgumentException`
- Controller catches and returns 400

### Database Connection Errors
- Spring Boot handles connection failures
- Check `docker-compose ps` if database errors occur
- Verify application.yaml credentials match docker-compose.yml

## Gotchas and Critical Considerations

### 1. SpringDoc Version Compatibility
**CRITICAL:** Must use springdoc-openapi-starter-webmvc-ui version 2.8.9 or later for Spring Boot 3.5.x. Earlier versions cause `NoSuchMethodError` due to Spring Framework 6.2.x API changes.

### 2. Lombok Annotation Processing
- Already configured in `pom.xml:64-70`
- If IDE shows errors, ensure annotation processing is enabled
- Maven build will work regardless of IDE setup

### 3. Password Storage
**IMPORTANT:** Current implementation stores passwords as plain text. If security is a concern:
- Add Spring Security dependency
- Use `BCryptPasswordEncoder` in service layer
- Update tests accordingly

### 4. Database Table Name
- Entity uses `@Table(name = "users")` because "user" is a reserved keyword in PostgreSQL
- Always use plural form for table names to avoid conflicts

### 5. Email Uniqueness
- Database enforces uniqueness via `unique = true` in `@Column`
- Service layer checks before insert/update to provide better error messages
- Concurrent inserts may still cause `DataIntegrityViolationException`

### 6. Validation Execution Order
- Jakarta Bean Validation runs before method execution
- Returns 400 automatically if validation fails
- Custom `@StrongPassword` requires regex: min 8 chars, uppercase, lowercase, digit, special char

### 7. Docker Compose
- Run `docker-compose up -d` before starting application
- Data persists in Docker volume `postgres_data`
- To reset: `docker-compose down -v` (destroys data)

### 8. Testing with H2
- Repository tests use H2 (in-memory) automatically via `@DataJpaTest`
- Controller tests use `@WebMvcTest` (no database needed)
- Integration tests can use Testcontainers for real PostgreSQL

### 9. API Documentation Access
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- These paths are configurable in application.yaml

### 10. Transaction Management
- Service methods use `@Transactional` for write operations
- Spring handles rollback on exceptions automatically
- Read operations don't need `@Transactional` but can benefit from it

## Quality Checklist

- [ ] All dependencies added to pom.xml (SpringDoc 2.8.15, validation)
- [ ] docker-compose.yml created with PostgreSQL 16
- [ ] application.yaml configured with database connection
- [ ] User entity created with all fields and validation
- [ ] Custom @StrongPassword annotation and validator implemented
- [ ] UserRepository created extending JpaRepository
- [ ] UserService created with all CRUD operations
- [ ] UserController created with proper REST endpoints
- [ ] GlobalExceptionHandler created for validation errors
- [ ] DTOs created for request/response separation
- [ ] Repository tests created with @DataJpaTest
- [ ] Controller tests created with @WebMvcTest
- [ ] All validation gates pass (compile, test, package)
- [ ] Application starts without errors
- [ ] Swagger UI accessible and shows User Management APIs
- [ ] All CRUD operations work via curl/Postman
- [ ] Validation returns 400 for invalid input
- [ ] Email uniqueness enforced

## Confidence Score: 8/10

**Rationale:**
- **Strong foundation**: Clear requirements, comprehensive research, detailed code examples
- **Well-documented**: All patterns explained, gotchas identified, validation gates executable
- **Spring Boot 3.5.x specifics**: Critical version compatibility issues documented
- **Complete implementation path**: Every file specified with full code

**Risk factors (-2 points):**
1. **First module in codebase**: No existing patterns to follow, may encounter unexpected project-specific configurations
2. **Password hashing ambiguity**: Requirements don't specify if passwords should be hashed; implementation assumes plain text but notes security concern

**Mitigation:**
- All validation gates are executable and comprehensive
- Error messages guide fixes for common issues
- External research provides fallback resources
- Gotchas section covers major pitfalls

This PRP provides sufficient context for one-pass implementation by an AI agent with access to web search and the codebase.
