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
