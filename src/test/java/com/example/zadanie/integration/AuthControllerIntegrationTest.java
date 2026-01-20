package com.example.zadanie.integration;

import com.example.zadanie.api.dto.LoginRequest;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        // Given - Create a user with encoded password
        String rawPassword = "Pass@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(null, "John Doe", "john@example.com", encodedPassword);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("john@example.com", rawPassword);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void shouldReturnBadRequestForInvalidPassword() throws Exception {
        // Given
        String rawPassword = "Pass@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(null, "John Doe", "john@example.com", encodedPassword);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("john@example.com", "WrongPassword@123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void shouldReturnBadRequestForNonExistentUser() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "Pass@123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void shouldReturnBadRequestForInvalidEmailFormat() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("invalid-email", "Pass@123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingEmail() throws Exception {
        // Given
        String requestJson = "{\"password\":\"Pass@123\"}";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingPassword() throws Exception {
        // Given
        String requestJson = "{\"email\":\"john@example.com\"}";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForEmptyCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("", "");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnValidJwtTokenOnSuccessfulLogin() throws Exception {
        // Given
        String rawPassword = "Pass@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(null, "John Doe", "john@example.com", encodedPassword);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("john@example.com", rawPassword);

        // When & Then - Verify token format (JWT has 3 parts separated by dots)
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.matchesRegex("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$")));
    }
}