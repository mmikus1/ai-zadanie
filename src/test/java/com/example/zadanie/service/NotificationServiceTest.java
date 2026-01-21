package com.example.zadanie.service;

import com.example.zadanie.entity.*;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.example.zadanie.repository.NotificationRepository;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderCompletedEvent completedEvent;
    private OrderExpiredEvent expiredEvent;

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

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.COMPLETED);

        completedEvent = new OrderCompletedEvent(
            1L, 1L, 1L, 2, new BigDecimal("199.98"), LocalDateTime.now()
        );

        expiredEvent = new OrderExpiredEvent(
            1L, 1L, 1L, 2, new BigDecimal("199.98"), LocalDateTime.now()
        );
    }

    @Test
    void shouldCreateOrderCompletedNotification() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // When
        Notification notification = notificationService.createOrderCompletedNotification(completedEvent);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
        assertThat(notification.getEmailSent()).isTrue();
        assertThat(notification.getMessage()).contains("completed successfully");
        verify(notificationRepository, times(2)).save(any(Notification.class)); // Once before email, once after
        verify(orderRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldCreateOrderExpiredNotification() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // When
        Notification notification = notificationService.createOrderExpiredNotification(expiredEvent);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_EXPIRED);
        assertThat(notification.getEmailSent()).isFalse(); // No email for expired orders
        assertThat(notification.getMessage()).contains("expired");
        verify(notificationRepository, times(1)).save(any(Notification.class)); // Only once (no email)
        verify(orderRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createOrderCompletedNotification(completedEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createOrderCompletedNotification(completedEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }
}
