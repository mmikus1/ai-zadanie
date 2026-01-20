package com.example.zadanie.integration;

import com.example.zadanie.api.dto.OrderCreateRequest;
import com.example.zadanie.api.dto.OrderUpdateRequest;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Create test user and product
        testUser = new User(null, "John Doe", "john@example.com", "Pass@123");
        testUser = userRepository.save(testUser);

        testProduct = new Product(null, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), 10, null);
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldGetAllOrders() throws Exception {
        // Given
        Order order1 = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order order2 = new Order(null, testUser, testProduct, 1, new BigDecimal("1299.99"), OrderStatus.COMPLETED, null, null);
        orderRepository.save(order1);
        orderRepository.save(order2);

        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].total").value(2599.98))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].quantity").value(1))
                .andExpect(jsonPath("$[1].total").value(1299.99))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldGetOrderById() throws Exception {
        // Given
        Order order = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order savedOrder = orderRepository.save(order);

        // When & Then
        mockMvc.perform(get("/api/orders/" + savedOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.total").value(2599.98))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldGetOrdersByUserId() throws Exception {
        // Given - Create another user
        User anotherUser = new User(null, "Jane Doe", "jane@example.com", "Pass@456");
        anotherUser = userRepository.save(anotherUser);

        Order order1 = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order order2 = new Order(null, testUser, testProduct, 1, new BigDecimal("1299.99"), OrderStatus.COMPLETED, null, null);
        Order order3 = new Order(null, anotherUser, testProduct, 3, new BigDecimal("3899.97"), OrderStatus.PENDING, null, null);
        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        // When & Then - Get orders for testUser
        mockMvc.perform(get("/api/orders/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[1].quantity").value(1));
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldGetOrdersByStatus() throws Exception {
        // Given
        Order order1 = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order order2 = new Order(null, testUser, testProduct, 1, new BigDecimal("1299.99"), OrderStatus.COMPLETED, null, null);
        Order order3 = new Order(null, testUser, testProduct, 3, new BigDecimal("3899.97"), OrderStatus.PENDING, null, null);
        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        // When & Then - Get PENDING orders
        mockMvc.perform(get("/api/orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        // When & Then - Get COMPLETED orders
        mockMvc.perform(get("/api/orders/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void shouldCreateOrderWithValidData() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(testUser.getId(), testProduct.getId(), 2);

        // When
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.total").value(2599.98))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        // Then - Verify it's persisted in the database
        assertThat(orderRepository.count()).isEqualTo(1);
        Order savedOrder = orderRepository.findAll().get(0);
        assertThat(savedOrder.getQuantity()).isEqualTo(2);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("2599.98"));
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForInvalidUserId() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(999L, testProduct.getId(), 2);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForInvalidProductId() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(testUser.getId(), 999L, 2);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForZeroQuantity() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(testUser.getId(), testProduct.getId(), 0);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForNegativeQuantity() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(testUser.getId(), testProduct.getId(), -5);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldUpdateOrder() throws Exception {
        // Given
        Order order = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order savedOrder = orderRepository.save(order);

        OrderUpdateRequest updateRequest = new OrderUpdateRequest(3, OrderStatus.PROCESSING);

        // When
        mockMvc.perform(put("/api/orders/" + savedOrder.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.total").value(3899.97))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        // Then - Verify it's updated in the database
        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getQuantity()).isEqualTo(3);
        assertThat(updatedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("3899.97"));
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldUpdateOrderStatusOnly() throws Exception {
        // Given
        Order order = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order savedOrder = orderRepository.save(order);

        OrderUpdateRequest updateRequest = new OrderUpdateRequest(null, OrderStatus.COMPLETED);

        // When
        mockMvc.perform(put("/api/orders/" + savedOrder.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenUpdatingNonExistentOrder() throws Exception {
        // Given
        OrderUpdateRequest updateRequest = new OrderUpdateRequest(3, OrderStatus.PROCESSING);

        // When & Then
        mockMvc.perform(put("/api/orders/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldDeleteOrder() throws Exception {
        // Given
        Order order = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order savedOrder = orderRepository.save(order);

        // When
        mockMvc.perform(delete("/api/orders/" + savedOrder.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Then - Verify it's deleted from the database
        assertThat(orderRepository.findById(savedOrder.getId())).isEmpty();
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenDeletingNonExistentOrder() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/orders/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoOrders() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListForUserWithNoOrders() throws Exception {
        // Given - User with no orders
        User newUser = new User(null, "New User", "new@example.com", "Pass@789");
        newUser = userRepository.save(newUser);

        // When & Then
        mockMvc.perform(get("/api/orders/user/" + newUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListForStatusWithNoOrders() throws Exception {
        // Given - Create order with PENDING status only
        Order order = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        orderRepository.save(order);

        // When & Then - Get EXPIRED orders (none exist)
        mockMvc.perform(get("/api/orders/status/EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void shouldCalculateTotalCorrectly() throws Exception {
        // Given - Product with specific price
        OrderCreateRequest request = new OrderCreateRequest(testUser.getId(), testProduct.getId(), 5);

        // When & Then - Total should be 5 * 1299.99 = 6499.95
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.total").value(6499.95));
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldRecalculateTotalWhenQuantityUpdated() throws Exception {
        // Given - Order with quantity 2
        Order order = new Order(null, testUser, testProduct, 2, new BigDecimal("2599.98"), OrderStatus.PENDING, null, null);
        Order savedOrder = orderRepository.save(order);

        // When - Update quantity to 4
        OrderUpdateRequest updateRequest = new OrderUpdateRequest(4, null);

        mockMvc.perform(put("/api/orders/" + savedOrder.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.total").value(5199.96));
    }
}