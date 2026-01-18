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
