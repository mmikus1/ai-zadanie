package com.example.zadanie.integration;

import com.example.zadanie.api.dto.UserCreateRequest;
import com.example.zadanie.api.dto.UserUpdateRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void shouldGetAllUsers() throws Exception {
        // Given
        User user1 = new User(null, "John Doe", "john@example.com", "Pass@123");
        User user2 = new User(null, "Jane Smith", "jane@example.com", "Pass@456");
        userRepository.save(user1);
        userRepository.save(user2);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].name").value("Jane Smith"))
                .andExpect(jsonPath("$[1].email").value("jane@example.com"));
    }

    @Test
    @WithMockUser
    void shouldGetUserById() throws Exception {
        // Given
        User user = new User(null, "John Doe", "john@example.com", "Pass@123");
        User savedUser = userRepository.save(user);

        // When & Then
        mockMvc.perform(get("/api/users/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldCreateUserWithValidData() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest("John Doe", "john@example.com", "Pass@123");

        // When
        String response = mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        // Then - Verify it's persisted in the database
        assertThat(userRepository.count()).isEqualTo(1);
        User savedUser = userRepository.findAll().get(0);
        assertThat(savedUser.getName()).isEqualTo("John Doe");
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForInvalidEmail() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest("John Doe", "invalid-email", "Pass@123");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForWeakPassword() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest("John Doe", "john@example.com", "weak");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForDuplicateEmail() throws Exception {
        // Given - existing user
        User existingUser = new User(null, "Existing User", "john@example.com", "Pass@123");
        userRepository.save(existingUser);

        // When - try to create user with same email
        UserCreateRequest request = new UserCreateRequest("John Doe", "john@example.com", "Pass@456");

        // Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify only one user exists
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    void shouldUpdateUser() throws Exception {
        // Given
        User user = new User(null, "John Doe", "john@example.com", "Pass@123");
        User savedUser = userRepository.save(user);

        UserUpdateRequest updateRequest = new UserUpdateRequest("John Updated", "john.updated@example.com");

        // When
        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"));

        // Then - Verify it's updated in the database
        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("John Updated");
        assertThat(updatedUser.getEmail()).isEqualTo("john.updated@example.com");
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenUpdatingNonExistentUser() throws Exception {
        // Given
        UserUpdateRequest updateRequest = new UserUpdateRequest("John Updated", "john.updated@example.com");

        // When & Then
        mockMvc.perform(put("/api/users/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldDeleteUser() throws Exception {
        // Given
        User user = new User(null, "John Doe", "john@example.com", "Pass@123");
        User savedUser = userRepository.save(user);

        // When
        mockMvc.perform(delete("/api/users/" + savedUser.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Then - Verify it's deleted from the database
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenDeletingNonExistentUser() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/users/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}