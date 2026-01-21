package com.example.zadanie.repository;

import com.example.zadanie.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Create test user
        testUser = new User(null, "Test User", "test@example.com", "password");
        testUser = userRepository.save(testUser);

        // Create test product
        Product testProduct = new Product(null, "Test Product", "Description", new BigDecimal("99.99"), 10, null);
        testProduct = productRepository.save(testProduct);

        // Create test order
        testOrder = new Order(null, testUser, testProduct, 2, new BigDecimal("199.98"), OrderStatus.COMPLETED, null, null);
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    void shouldSaveNotification() {
        // Given
        Notification notification = new Notification();
        notification.setOrder(testOrder);
        notification.setUser(testUser);
        notification.setNotificationType(NotificationType.ORDER_COMPLETED);
        notification.setMessage("Test notification");
        notification.setEmailSent(true);

        // When
        Notification saved = notificationRepository.save(notification);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrder().getId()).isEqualTo(testOrder.getId());
        assertThat(saved.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByUserId() {
        // Given
        Notification notification1 = new Notification(null, testOrder, testUser, NotificationType.ORDER_COMPLETED, "Message 1", true, null);
        Notification notification2 = new Notification(null, testOrder, testUser, NotificationType.ORDER_EXPIRED, "Message 2", false, null);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // When
        List<Notification> notifications = notificationRepository.findByUserId(testUser.getId());

        // Then
        assertThat(notifications).hasSize(2);
    }

    @Test
    void shouldFindByOrderId() {
        // Given
        Notification notification = new Notification(null, testOrder, testUser, NotificationType.ORDER_COMPLETED, "Message", true, null);
        notificationRepository.save(notification);

        // When
        List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());

        // Then
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getOrder().getId()).isEqualTo(testOrder.getId());
    }

    @Test
    void shouldFindByNotificationType() {
        // Given
        Notification notification1 = new Notification(null, testOrder, testUser, NotificationType.ORDER_COMPLETED, "Message 1", true, null);
        Notification notification2 = new Notification(null, testOrder, testUser, NotificationType.ORDER_EXPIRED, "Message 2", false, null);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // When
        List<Notification> completedNotifications = notificationRepository.findByNotificationType(NotificationType.ORDER_COMPLETED);

        // Then
        assertThat(completedNotifications).hasSize(1);
        assertThat(completedNotifications.get(0).getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
    }
}
