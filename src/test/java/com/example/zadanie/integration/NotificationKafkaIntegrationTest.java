package com.example.zadanie.integration;

import com.example.zadanie.entity.*;
import com.example.zadanie.repository.NotificationRepository;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
import com.example.zadanie.service.OrderEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(topics = {"order-events"}, partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"})
@DirtiesContext
class NotificationKafkaIntegrationTest {

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Clean up
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Create test data
        testUser = new User(null, "Test User", "test@example.com", "password");
        testUser = userRepository.save(testUser);

        testProduct = new Product(null, "Test Product", "Description", new BigDecimal("99.99"), 10, null);
        testProduct = productRepository.save(testProduct);

        testOrder = new Order(null, testUser, testProduct, 2, new BigDecimal("199.98"), OrderStatus.COMPLETED, null, null);
        testOrder = orderRepository.save(testOrder);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateNotificationWhenOrderCompletedEventPublished() {
        // When
        orderEventProducer.publishOrderCompleted(testOrder);

        // Then - wait for async processing (up to 10 seconds)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());
            assertThat(notifications).hasSize(1);

            Notification notification = notifications.get(0);
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_COMPLETED);
            assertThat(notification.getEmailSent()).isTrue();
            assertThat(notification.getMessage()).contains("completed successfully");
            assertThat(notification.getUser().getId()).isEqualTo(testUser.getId());
        });
    }

    @Test
    void shouldCreateNotificationWhenOrderExpiredEventPublished() {
        // Given
        testOrder.setStatus(OrderStatus.EXPIRED);
        testOrder = orderRepository.save(testOrder);

        // When
        orderEventProducer.publishOrderExpired(testOrder);

        // Then - wait for async processing (up to 10 seconds)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());
            assertThat(notifications).hasSize(1);

            Notification notification = notifications.get(0);
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_EXPIRED);
            assertThat(notification.getEmailSent()).isFalse(); // No email for expired
            assertThat(notification.getMessage()).contains("expired");
            assertThat(notification.getUser().getId()).isEqualTo(testUser.getId());
        });
    }

    @Test
    void shouldNotCreateNotificationForOrderCreatedEvent() {
        // When
        orderEventProducer.publishOrderCreated(testOrder);

        // Then - wait 3 seconds (should not create notification)
        await().pollDelay(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByOrderId(testOrder.getId());
            assertThat(notifications).isEmpty(); // NotificationEventConsumer ignores order-created
        });
    }
}
