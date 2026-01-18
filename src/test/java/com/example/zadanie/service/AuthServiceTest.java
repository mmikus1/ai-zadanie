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
