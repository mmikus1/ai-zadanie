# PRP: Authentication Module with JWT

## Context and Overview

This PRP defines the implementation of a JWT-based authentication module for the existing Spring Boot application. The user management module (CRUD operations) already exists, and we need to add login functionality.

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database (already configured)
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction

**Feature Requirements:**
1. New AuthController with login endpoint (separate from UserController)
2. Endpoint accepts email and password
3. Validates credentials against database (User entity already exists)
4. Returns JWT token on successful authentication
5. Returns 400 error for invalid credentials
6. Implement password hashing (currently passwords are stored in plain text)

**Existing Codebase Context:**
- User entity exists at `src/main/java/com/example/zadanie/entity/User.java:1-36`
- UserRepository with `findByEmail()` method exists at `src/main/java/com/example/zadanie/repository/UserRepository.java:11`
- UserService exists at `src/main/java/com/example/zadanie/service/UserService.java:1-70`
- UserController pattern to follow at `src/main/java/com/example/zadanie/api/controller/UserController.java:1-73`
- GlobalExceptionHandler exists at `src/main/java/com/example/zadanie/api/error/GlobalExceptionHandler.java:1-27`
- DTOs follow pattern at `src/main/java/com/example/zadanie/api/dto/UserCreateRequest.java:1-28`
- Tests follow patterns at `src/test/java/com/example/zadanie/controller/UserControllerTest.java:1-67`

## Research Findings and Documentation

### Spring Security 6 and JWT with Spring Boot 3

**Critical Resources:**
- [Spring Security 6.x Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT Authentication Tutorial - Medium (Spring Boot 3 + Security 6)](https://medium.com/@truongbui95/jwt-authentication-and-authorization-with-spring-boot-3-and-spring-security-6-2f90f9337421)
- [Spring Boot 3 JWT Implementation Guide - Medium](https://medium.com/spring-boot/spring-boot-3-spring-security-6-jwt-authentication-authorization-98702d6313a5)
- [JWT Authentication Tutorial by Tericcabrel](https://blog.tericcabrel.com/jwt-authentication-springboot-spring-security/)
- [GitHub - Spring Boot 3 JWT Security Sample](https://github.com/ali-bouali/spring-boot-3-jwt-security)
- [GitHub - Spring Security 6 JWT with PostgreSQL](https://github.com/MossaabFrifita/spring-boot-3-security-6-jwt)

### JJWT Library (JSON Web Token for Java)

**Version to use:** 0.12.6 (latest stable, compatible with Spring Boot 3.5.9 and Java 21)

**Required Dependencies (3 artifacts):**
- `jjwt-api`: Core API for JWT operations
- `jjwt-impl`: Implementation of the JWT API
- `jjwt-jackson`: JSON serialization using Jackson

**Resources:**
- [JJWT GitHub Repository](https://github.com/jwtk/jjwt)
- [JJWT Maven Central](https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-api)
- [Spring Boot JWT with Java 21 Tutorial - Medium](https://medium.com/@birudeo.garande.bmc/spring-boot-jwt-authentication-implementation-with-java-21-d61070d99858)
- [JWT Implementation Tutorial - Medium](https://medium.com/@tericcabrel/implement-jwt-authentication-in-a-spring-boot-3-application-5839e4fd8fac)
- [JWT.io - Introduction to JSON Web Tokens](https://jwt.io/introduction)

### BCrypt Password Encoding

**Resources:**
- [BCryptPasswordEncoder Documentation](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)
- [Spring Security Password Encoding with BCrypt - Baeldung](https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt)
- [Password Hashing using BCrypt - Medium](https://tharushkaheshan.medium.com/password-hashing-using-bcrypt-in-spring-security-part-8-d867a83b8695)
- [Spring Boot BCrypt Password Encoding - DevGlan](https://www.devglan.com/spring-security/spring-boot-security-password-encoding-bcrypt-encoder)
- [Password Handling with Spring Boot - Reflectoring](https://reflectoring.io/spring-security-password-handling/)

**Key Points:**
- BCrypt strength parameter: 10-14 (default 10, higher = more secure but slower)
- BCrypt hashes start with `$2a$`, `$2b$`, or `$2y$`
- Each hash is unique even for same password (includes random salt)
- Manual configuration required in Spring Boot 3 (no auto-configuration)

### Spring Boot 3.5.9 Specific Considerations

- Uses Jakarta EE (jakarta.*), not javax.*
- Requires Spring Security 6.x if using Spring Security
- For simple JWT without full security framework, use only `spring-security-crypto` for BCrypt
- Compatible with Java 21

## Implementation Blueprint

### High-Level Approach

```
1. Dependency Setup
   ├── Add JJWT dependencies (api, impl, jackson) - version 0.12.6
   ├── Add spring-security-crypto for BCryptPasswordEncoder
   └── Configure JWT secret in application.yaml

2. Password Security Layer
   ├── Create SecurityConfig with PasswordEncoder bean
   ├── Update UserService.createUser() to hash passwords
   └── Document password migration for existing users

3. JWT Service Layer
   ├── Create JwtService for token generation
   ├── Implement token validation/parsing
   ├── Configure expiration and signing key
   └── Extract claims (email, user id)

4. Authentication Service Layer
   ├── Create AuthService with login logic
   ├── Validate email/password against database
   ├── Use BCrypt to verify password
   └── Generate JWT token on success

5. API Layer (REST)
   ├── Create LoginRequest DTO with validation
   ├── Create LoginResponse DTO with token
   ├── Create AuthController with /login endpoint
   └── Add OpenAPI annotations for documentation

6. Error Handling
   ├── Create custom AuthenticationException
   ├── Update GlobalExceptionHandler for 400 responses
   └── Return meaningful error messages

7. Testing
   ├── Create JwtServiceTest (unit tests)
   ├── Create AuthServiceTest (unit tests)
   ├── Create AuthControllerTest (integration tests)
   ├── Test password hashing in UserService
   └── Test invalid credentials scenarios

8. Validation & Documentation
   ├── Verify compilation
   ├── Test login endpoint with curl
   ├── Verify JWT token structure
   └── Update Swagger UI
```

### Package Structure (New Components)

```
com.example.zadanie/
├── api/
│   ├── controller/
│   │   ├── UserController.java (existing)
│   │   └── AuthController.java (NEW)
│   ├── dto/
│   │   ├── UserCreateRequest.java (existing)
│   │   ├── LoginRequest.java (NEW)
│   │   └── LoginResponse.java (NEW)
│   └── error/
│       └── GlobalExceptionHandler.java (UPDATE)
├── service/
│   ├── UserService.java (UPDATE - add password hashing)
│   ├── AuthService.java (NEW)
│   └── JwtService.java (NEW)
├── config/
│   └── SecurityConfig.java (NEW)
├── exception/
│   └── AuthenticationException.java (NEW)
└── entity/
    └── User.java (existing)
```

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Add Dependencies to pom.xml

Add the following dependencies after the existing dependencies (after line 73):

```xml
<!-- JWT Dependencies -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security Crypto (for BCryptPasswordEncoder) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

**Location:** Add after line 73 in `pom.xml`

### Task 2: Configure JWT Settings in application.yaml

Add JWT configuration to `src/main/resources/application.yaml`:

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
  expiration: 86400000  # 24 hours in milliseconds
```

**Important:** The secret should be at least 256 bits (32 bytes) for HS256 algorithm. The default value is a base64-encoded example - in production, use environment variable `JWT_SECRET`.

Add this after the `springdoc` section (after line 24 in existing application.yaml).

### Task 3: Create Security Configuration

Create `src/main/java/com/example/zadanie/config/SecurityConfig.java`:

```java
package com.example.zadanie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

**Explanation:**
- `@Configuration`: Marks this as a Spring configuration class
- `PasswordEncoder` bean: Used for hashing and verifying passwords
- BCrypt strength of 10: Good balance between security and performance

### Task 4: Create JWT Service

Create `src/main/java/com/example/zadanie/service/JwtService.java`:

```java
package com.example.zadanie.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Generate JWT token for a user
     */
    public String generateToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        return createToken(claims, email);
    }

    /**
     * Create JWT token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract username (email) from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
```

**Key Points:**
- Uses JJWT 0.12.6 API (`Jwts.builder()`, `signWith()`, `verifyWith()`)
- Configurable secret and expiration from application.yaml
- Includes user ID and email in claims
- Provides validation and extraction methods

### Task 5: Create Custom Authentication Exception

Create `src/main/java/com/example/zadanie/exception/AuthenticationException.java`:

```java
package com.example.zadanie.exception;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
```

### Task 6: Create DTOs for Authentication

Create `src/main/java/com/example/zadanie/api/dto/LoginRequest.java`:

```java
package com.example.zadanie.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Password is required")
    private String password;
}
```

Create `src/main/java/com/example/zadanie/api/dto/LoginResponse.java`:

```java
package com.example.zadanie.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private String email;
    private Long userId;

    public LoginResponse(String token, String email, Long userId) {
        this.token = token;
        this.email = email;
        this.userId = userId;
    }
}
```

### Task 7: Create Authentication Service

Create `src/main/java/com/example/zadanie/service/AuthService.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.api.dto.LoginRequest;
import com.example.zadanie.api.dto.LoginResponse;
import com.example.zadanie.entity.User;
import com.example.zadanie.exception.AuthenticationException;
import com.example.zadanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId());

        // Return response with token
        return new LoginResponse(token, user.getEmail(), user.getId());
    }
}
```

**Key Points:**
- Uses `PasswordEncoder.matches()` to verify hashed password
- Returns generic "Invalid email or password" for security (don't reveal which field is wrong)
- Generates JWT token with user email and ID
- Throws `AuthenticationException` for invalid credentials

### Task 8: Update UserService to Hash Passwords

Update `src/main/java/com/example/zadanie/service/UserService.java`:

Add `PasswordEncoder` dependency and update `createUser` method:

```java
// Add at the top of the class (around line 18):
private final PasswordEncoder passwordEncoder;

// Update createUser method (replace lines 29-40):
@Transactional
public User createUser(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("Email already exists: " + request.getEmail());
    }

    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password

    return userRepository.save(user);
}
```

**Full updated UserService.java constructor (line 15-18):**
```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // ADD THIS LINE
```

**IMPORTANT:** This change means new users will have hashed passwords, but existing users with plain-text passwords will NOT be able to log in. See "Gotchas" section for migration strategy.

### Task 9: Create Authentication Controller

Create `src/main/java/com/example/zadanie/api/controller/AuthController.java`:

```java
package com.example.zadanie.api.controller;

import com.example.zadanie.api.dto.LoginRequest;
import com.example.zadanie.api.dto.LoginResponse;
import com.example.zadanie.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

**Key Points:**
- Follows existing controller pattern (UserController.java:17-73)
- Uses `@Valid` for request validation
- OpenAPI annotations for Swagger documentation
- Returns 200 OK with token on success
- Exceptions handled by GlobalExceptionHandler

### Task 10: Update GlobalExceptionHandler

Update `src/main/java/com/example/zadanie/api/error/GlobalExceptionHandler.java`:

Add handler for `AuthenticationException`:

```java
// Add import at top:
import com.example.zadanie.exception.AuthenticationException;
import java.util.Collections;

// Add this method after the existing handleValidationExceptions method (after line 26):
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<Map<String, String>> handleAuthenticationException(
        AuthenticationException ex) {
    Map<String, String> error = Collections.singletonMap("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
}
```

**Note:** User requirement specifies 400 (BAD_REQUEST) for invalid login. Standard practice is 401 (UNAUTHORIZED), but we follow the requirement.

### Task 11: Create Tests for JWT Service

Create `src/test/java/com/example/zadanie/service/JwtServiceTest.java`:

```java
package com.example.zadanie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtService, "secretKey",
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void shouldGenerateTokenSuccessfully() {
        String token = jwtService.generateToken("test@example.com", 1L);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateToken("test@example.com", 1L);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String token = jwtService.generateToken("test@example.com", 1L);
        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        String token = jwtService.generateToken("test@example.com", 1L);
        Boolean isValid = jwtService.validateToken(token, "test@example.com");

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidUsername() {
        String token = jwtService.generateToken("test@example.com", 1L);
        Boolean isValid = jwtService.validateToken(token, "wrong@example.com");

        assertThat(isValid).isFalse();
    }

    @Test
    void shouldDetectTokenNotExpired() {
        String token = jwtService.generateToken("test@example.com", 1L);
        Boolean isExpired = jwtService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }
}
```

### Task 12: Create Tests for Authentication Service

Create `src/test/java/com/example/zadanie/service/AuthServiceTest.java`:

```java
package com.example.zadanie.service;

import com.example.zadanie.api.dto.LoginRequest;
import com.example.zadanie.api.dto.LoginResponse;
import com.example.zadanie.entity.User;
import com.example.zadanie.exception.AuthenticationException;
import com.example.zadanie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "Test User", "test@example.com", "$2a$10$hashedPassword");
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken("test@example.com", 1L)).thenReturn("jwt.token.here");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        LoginRequest request = new LoginRequest("notfound@example.com", "password123");

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Invalid email or password");
    }

    @Test
    void shouldThrowExceptionWhenPasswordIncorrect() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Invalid email or password");
    }
}
```

### Task 13: Create Tests for Authentication Controller

Create `src/test/java/com/example/zadanie/controller/AuthControllerTest.java`:

```java
package com.example.zadanie.controller;

import com.example.zadanie.api.controller.AuthController;
import com.example.zadanie.api.dto.LoginRequest;
import com.example.zadanie.api.dto.LoginResponse;
import com.example.zadanie.exception.AuthenticationException;
import com.example.zadanie.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        LoginResponse response = new LoginResponse("jwt.token.here", "test@example.com", 1L);

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt.token.here"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldReturnBadRequestForInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(authService.login(any())).thenThrow(new AuthenticationException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void shouldReturnBadRequestForInvalidEmail() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingPassword() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", null);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

### Task 14: Update User Service Tests

The existing `UserServiceTest` (if it exists) or create integration tests to verify password hashing works correctly.

Add test to verify passwords are hashed:

```java
@Test
void shouldHashPasswordWhenCreatingUser() {
    UserCreateRequest request = new UserCreateRequest("John Doe", "john@example.com", "Password@123");

    User created = userService.createUser(request);

    assertThat(created.getPassword()).isNotEqualTo("Password@123"); // Should be hashed
    assertThat(created.getPassword()).startsWith("$2a$"); // BCrypt hash prefix
}
```

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Clean and compile the project
./mvnw clean compile

# Expected output: BUILD SUCCESS

# 2. Run all tests
./mvnw test

# Expected output: All tests pass (including new JWT, Auth service, and Auth controller tests)

# 3. Package the application
./mvnw package

# 4. Start the application
./mvnw spring-boot:run

# Expected output: Application starts on port 8080

# 5. Test Login API - Create a test user first
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"testuser@example.com","password":"Test@1234"}'

# Expected output: User created with hashed password

# 6. Test Login - Valid credentials
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"Test@1234"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 200 OK with JWT token
# Example response:
# {
#   "token": "eyJhbGciOiJIUzI1NiJ9...",
#   "type": "Bearer",
#   "email": "testuser@example.com",
#   "userId": 1
# }

# 7. Test Login - Invalid email
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"wrong@example.com","password":"Test@1234"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 400 BAD REQUEST
# {"error": "Invalid email or password"}

# 8. Test Login - Invalid password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"wrongpassword"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 400 BAD REQUEST
# {"error": "Invalid email or password"}

# 9. Test Login - Invalid email format
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"invalid-email","password":"Test@1234"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 400 BAD REQUEST with validation error

# 10. Test Login - Missing password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com"}' \
  -w "\nStatus: %{http_code}\n"

# Expected output: 400 BAD REQUEST with validation error

# 11. Verify JWT token structure (copy token from step 6)
# Paste the token at https://jwt.io to decode
# Expected: Header with HS256, payload with email and userId, valid signature

# 12. Verify Swagger UI
# Open browser: http://localhost:8080/swagger-ui.html
# Expected: See "Authentication" section with POST /api/auth/login endpoint

# 13. Test password hashing - Check database
# If you have psql access:
# psql -h localhost -U zadanie_user -d zadanie_db
# SELECT id, email, password FROM users;
# Expected: Password column shows hashed values starting with $2a$
```

## Error Handling Strategy

### Authentication Errors (400 Bad Request per requirement)

**Scenario 1: Invalid Email (user not found)**
- Exception: `AuthenticationException("Invalid email or password")`
- HTTP Status: 400
- Response Body: `{"error": "Invalid email or password"}`

**Scenario 2: Invalid Password**
- Exception: `AuthenticationException("Invalid email or password")`
- HTTP Status: 400
- Response Body: `{"error": "Invalid email or password"}`

**Security Note:** Always return the same error message for invalid email or password to prevent user enumeration attacks.

### Validation Errors (400 Bad Request)

**Scenario 3: Invalid Email Format**
- Triggered by: `@Email` annotation in `LoginRequest`
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"email": "Email must be valid"}`

**Scenario 4: Missing Required Fields**
- Triggered by: `@NotNull` annotations in `LoginRequest`
- Handled by: `GlobalExceptionHandler.handleValidationExceptions()`
- HTTP Status: 400
- Response Body: `{"email": "Email is required"}` or `{"password": "Password is required"}`

### JWT Parsing Errors (Future Enhancement)

When implementing JWT validation in protected endpoints:
- Use try-catch around `jwtService.validateToken()`
- Catch `JwtException` and return 401 Unauthorized
- Return meaningful error: "Invalid or expired token"

## Gotchas and Critical Considerations

### 1. Password Migration for Existing Users

**CRITICAL ISSUE:** After implementing password hashing, existing users with plain-text passwords will NOT be able to log in.

**Problem:**
- Current User table has passwords stored as plain text (UserService.java:37)
- After update, `createUser()` will hash new passwords
- Login will use `passwordEncoder.matches()` which expects hashed passwords
- Plain-text passwords will fail the BCrypt match check

**Solutions:**

**Option A: Force Password Reset (Recommended for Production)**
```sql
-- Mark all existing users as needing password reset
ALTER TABLE users ADD COLUMN password_reset_required BOOLEAN DEFAULT FALSE;
UPDATE users SET password_reset_required = TRUE WHERE password NOT LIKE '$2a$%';
```

**Option B: Manual Migration Script**
```java
// Create a one-time migration endpoint (DELETE after migration)
@PostMapping("/admin/migrate-passwords")
public ResponseEntity<String> migratePasswords() {
    List<User> users = userRepository.findAll();
    for (User user : users) {
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
        }
    }
    return ResponseEntity.ok("Migrated " + users.size() + " users");
}
```

**Option C: Test Environment - Delete and Recreate Users**
```bash
# For development/testing only
curl -X DELETE http://localhost:8080/api/users/1
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"Test@1234"}'
```

**IMPORTANT:** Document this issue in the PRP execution notes.

### 2. JWT Secret Key Security

**Issues:**
- Default secret in application.yaml is for development only
- Production MUST use environment variable: `export JWT_SECRET=your-secure-secret-here`
- Secret must be at least 256 bits (32 bytes) for HS256
- Never commit production secrets to Git

**Best Practice:**
```yaml
# application.yaml
jwt:
  secret: ${JWT_SECRET:dev-secret-only-for-local}

# Production deployment:
export JWT_SECRET=$(openssl rand -base64 32)
```

### 3. BCrypt Performance

**Consideration:**
- BCrypt is intentionally slow (strength = 10 means 2^10 iterations)
- Login may take 100-300ms to verify password
- This is intentional security feature (prevents brute force)
- Don't reduce strength below 10 in production

**Impact:**
- High-traffic applications may need caching strategies
- Consider rate limiting on login endpoint
- Monitor login endpoint latency

### 4. JJWT Version Compatibility

**CRITICAL:** Must use JJWT 0.12.6 for Java 21 compatibility

**Breaking Changes from 0.11.x to 0.12.x:**
- `signWith(SignatureAlgorithm, Key)` → `signWith(Key)`
- `setExpiration()` → `expiration()`
- `setSubject()` → `subject()`
- `parserBuilder()` → `parser().verifyWith()`

**Current Implementation Uses 0.12.6 API**

### 5. Spring Security Dependency Scope

**Important:** We're using `spring-security-crypto` for BCryptPasswordEncoder only, NOT full Spring Security framework.

**Why:**
- Full Spring Security adds authentication filters, security chains, CSRF protection
- User requirement is simple: login endpoint returning JWT
- Adding full framework would require extensive configuration
- BCryptPasswordEncoder can be used standalone

**Future Enhancement:** If you need to protect other endpoints with JWT, you'll need:
- Full `spring-boot-starter-security` dependency
- Security filter chain configuration
- JWT authentication filter
- This PRP focuses on login endpoint only

### 6. Token Expiration and Refresh Tokens

**Current Implementation:**
- Token expires after 24 hours (configurable in application.yaml)
- No refresh token mechanism
- Client must re-login after expiration

**Future Enhancement:**
- Implement refresh tokens for better UX
- Store refresh tokens in database
- Endpoint: POST /api/auth/refresh with refresh token

### 7. CORS Configuration

**Issue:** If frontend is on different domain/port, you'll need CORS configuration.

**Solution (if needed):**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### 8. Testing with BCrypt

**Issue:** BCrypt generates different hashes each time, even for same password.

**Example:**
```java
passwordEncoder.encode("password123"); // $2a$10$abc...
passwordEncoder.encode("password123"); // $2a$10$xyz... (different!)
```

**Solution in Tests:**
- Use `passwordEncoder.matches()` instead of string equality
- Mock `passwordEncoder` in unit tests to return predictable values
- In integration tests, use real encoder but verify with `matches()`

### 9. Email Case Sensitivity

**Issue:** Email addresses are case-insensitive (test@example.com == Test@Example.Com).

**Current Implementation:** Uses exact case match.

**Best Practice Enhancement:**
```java
// In AuthService.login():
String normalizedEmail = request.getEmail().toLowerCase();
User user = userRepository.findByEmail(normalizedEmail)...

// In UserService.createUser():
user.setEmail(request.getEmail().toLowerCase());
```

**Consider:** Add this as a follow-up enhancement or document as known limitation.

### 10. Rate Limiting and Security Headers

**Not Implemented (Future Enhancement):**
- Rate limiting on login endpoint (prevent brute force)
- Account lockout after N failed attempts
- Security headers (X-Content-Type-Options, X-Frame-Options, etc.)
- Audit logging of login attempts

**Recommendation:** Document these as future security enhancements.

## Quality Checklist

- [ ] All dependencies added to pom.xml (JJWT 0.12.6, spring-security-crypto)
- [ ] JWT configuration added to application.yaml
- [ ] SecurityConfig created with PasswordEncoder bean
- [ ] JwtService created with token generation and validation
- [ ] AuthenticationException created
- [ ] LoginRequest DTO created with validation
- [ ] LoginResponse DTO created
- [ ] AuthService created with login logic
- [ ] UserService updated to hash passwords
- [ ] AuthController created with login endpoint
- [ ] GlobalExceptionHandler updated for AuthenticationException
- [ ] JwtServiceTest created with comprehensive tests
- [ ] AuthServiceTest created with unit tests
- [ ] AuthControllerTest created with integration tests
- [ ] All validation gates pass (compile, test, package)
- [ ] Application starts without errors
- [ ] Login endpoint returns JWT token for valid credentials
- [ ] Login endpoint returns 400 for invalid credentials
- [ ] JWT token includes email and userId claims
- [ ] Password hashing works for new users
- [ ] Swagger UI shows Authentication endpoints
- [ ] Password migration strategy documented

## Confidence Score: 8/10

**Rationale:**

**Strong Foundation (+):**
- Clear, specific requirements with existing codebase to reference
- Comprehensive research with multiple documentation sources
- Well-established patterns to follow (UserController, UserService, DTOs, tests)
- Complete code examples for all components
- Executable validation gates with expected outputs
- JJWT 0.12.6 and Spring Boot 3.5.9 compatibility confirmed

**Detailed Implementation (+):**
- Step-by-step tasks with exact file locations and line numbers
- Complete code snippets for every file
- Multiple test classes covering all scenarios
- Error handling strategy clearly defined

**Risk Factors (-):**

1. **Password Migration Complexity (-1)**
   - Existing users have plain-text passwords
   - Multiple migration strategies possible
   - Requires careful handling to avoid breaking existing users
   - Mitigation: Documented three migration options with examples

2. **JWT Secret Configuration (-0.5)**
   - Secret key management can be tricky
   - Environment variable setup required for production
   - Mitigation: Clear documentation and examples provided

3. **Testing BCrypt Logic (-0.5)**
   - BCrypt non-deterministic hashing can confuse tests
   - Requires mocking or integration testing
   - Mitigation: Test examples show proper mocking patterns

**Strengths:**
- All external dependencies researched with specific versions
- Spring Boot 3.5.9 and Java 21 compatibility verified
- Complete test coverage planned
- Security best practices documented
- Gotchas section covers all major pitfalls

**Expected Outcome:**
An AI agent with access to this PRP, the codebase, and web search should be able to implement the authentication module successfully in one pass, with clear validation gates to confirm success.

## Documentation Sources

This PRP was created using research from the following sources:

**Spring Security & JWT:**
- [Spring Security 6.x Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT Authentication Tutorial - Medium (Spring Boot 3 + Security 6)](https://medium.com/@truongbui95/jwt-authentication-and-authorization-with-spring-boot-3-and-spring-security-6-2f90f9337421)
- [Spring Boot 3 JWT Implementation Guide - Medium](https://medium.com/spring-boot/spring-boot-3-spring-security-6-jwt-authentication-authorization-98702d6313a5)
- [JWT Authentication Tutorial by Tericcabrel](https://blog.tericcabrel.com/jwt-authentication-springboot-spring-security/)
- [GitHub - Spring Boot 3 JWT Security Sample](https://github.com/ali-bouali/spring-boot-3-jwt-security)
- [GitHub - Spring Security 6 JWT with PostgreSQL](https://github.com/MossaabFrifita/spring-boot-3-security-6-jwt)

**JJWT Library:**
- [JJWT GitHub Repository](https://github.com/jwtk/jjwt)
- [JJWT Maven Central](https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-api)
- [Spring Boot JWT with Java 21 Tutorial - Medium](https://medium.com/@birudeo.garande.bmc/spring-boot-jwt-authentication-implementation-with-java-21-d61070d99858)
- [JWT Implementation Tutorial - Medium](https://medium.com/@tericcabrel/implement-jwt-authentication-in-a-spring-boot-3-application-5839e4fd8fac)
- [JWT.io - Introduction to JSON Web Tokens](https://jwt.io/introduction)

**BCrypt Password Encoding:**
- [BCryptPasswordEncoder Documentation](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)
- [Spring Security Password Encoding with BCrypt - Baeldung](https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt)
- [Password Hashing using BCrypt - Medium](https://tharushkaheshan.medium.com/password-hashing-using-bcrypt-in-spring-security-part-8-d867a83b8695)
- [Spring Boot BCrypt Password Encoding - DevGlan](https://www.devglan.com/spring-security/spring-boot-security-password-encoding-bcrypt-encoder)
- [Password Handling with Spring Boot - Reflectoring](https://reflectoring.io/spring-security-password-handling/)