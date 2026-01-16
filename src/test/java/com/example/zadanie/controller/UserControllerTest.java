package com.example.zadanie.controller;

import com.example.zadanie.api.controller.UserController;
import com.example.zadanie.api.dto.UserCreateRequest;
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
