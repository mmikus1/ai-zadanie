package com.example.zadanie.scheduler;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.service.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpirationSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderExpirationScheduler orderExpirationScheduler;

    private Order testOrder;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testProduct = new Product();
        testProduct.setId(1L);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PROCESSING);
    }

    @Test
    void shouldExpireOldOrders() {
        // Given
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testOrder));

        // When
        orderExpirationScheduler.expireOldOrders();

        // Then
        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class));
        verify(orderRepository).save(testOrder);
        verify(orderEventProducer).publishOrderExpired(testOrder);
        assert testOrder.getStatus() == OrderStatus.EXPIRED;
    }

    @Test
    void shouldDoNothingWhenNoOrdersToExpire() {
        // Given
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        orderExpirationScheduler.expireOldOrders();

        // Then
        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class));
        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).publishOrderExpired(any());
    }

    @Test
    void shouldExpireMultipleOrders() {
        // Given
        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(testUser);
        order1.setProduct(testProduct);
        order1.setQuantity(1);
        order1.setTotal(new BigDecimal("99.99"));
        order1.setStatus(OrderStatus.PROCESSING);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(testUser);
        order2.setProduct(testProduct);
        order2.setQuantity(2);
        order2.setTotal(new BigDecimal("199.98"));
        order2.setStatus(OrderStatus.PROCESSING);

        List<Order> orders = Arrays.asList(order1, order2);
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class)))
            .thenReturn(orders);

        // When
        orderExpirationScheduler.expireOldOrders();

        // Then
        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PROCESSING), any(LocalDateTime.class));
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderEventProducer, times(2)).publishOrderExpired(any(Order.class));
    }
}
