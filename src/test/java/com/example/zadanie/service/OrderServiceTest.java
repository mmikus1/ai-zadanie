package com.example.zadanie.service;

import com.example.zadanie.api.dto.OrderCreateRequest;
import com.example.zadanie.api.dto.OrderUpdateRequest;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldGetAllOrders() {
        // Given
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder));

        // When
        List<Order> orders = orderService.getAllOrders();

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getQuantity()).isEqualTo(2);
        verify(orderRepository).findAll();
    }

    @Test
    void shouldGetOrderById() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Optional<Order> found = orderService.getOrderById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
        verify(orderRepository).findById(1L);
    }

    @Test
    void shouldCreateOrder() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order created = orderService.createOrder(request);

        // Then
        assertThat(created).isNotNull();
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct); // Stock updated
        verify(orderRepository).save(any(Order.class));
        assertThat(testProduct.getStock()).isEqualTo(8); // 10 - 2 = 8
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(999L, 1L, 2);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(1L, 999L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void shouldThrowExceptionWhenStockInsufficient() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 20); // Request 20, but only 10 in stock
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldThrowExceptionWhenStockIsZero() {
        // Given
        testProduct.setStock(0);
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("out of stock");
    }

    @Test
    void shouldUpdateOrder() {
        // Given
        OrderUpdateRequest request = new OrderUpdateRequest(3, OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order updated = orderService.updateOrder(1L, request);

        // Then
        assertThat(updated).isNotNull();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingCompletedOrder() {
        // Given
        testOrder.setStatus(OrderStatus.COMPLETED);
        OrderUpdateRequest request = new OrderUpdateRequest(3, null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot update order with status: COMPLETED");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingExpiredOrder() {
        // Given
        testOrder.setStatus(OrderStatus.EXPIRED);
        OrderUpdateRequest request = new OrderUpdateRequest(3, null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot update order with status: EXPIRED");
    }

    @Test
    void shouldDeleteOrder() {
        // Given
        when(orderRepository.existsById(1L)).thenReturn(true);

        // When
        orderService.deleteOrder(1L);

        // Then
        verify(orderRepository).existsById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentOrder() {
        // Given
        when(orderRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> orderService.deleteOrder(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order not found");
    }
}
