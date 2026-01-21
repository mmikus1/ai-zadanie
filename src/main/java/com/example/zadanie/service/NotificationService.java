package com.example.zadanie.service;

import com.example.zadanie.entity.Notification;
import com.example.zadanie.entity.NotificationType;
import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.User;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.example.zadanie.repository.NotificationRepository;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createOrderCompletedNotification(OrderCompletedEvent event) {
        log.info("Creating notification for completed order {}", event.getOrderId());

        // Find order and user
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

        User user = userRepository.findById(event.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + event.getUserId()));

        // Build notification message
        String message = String.format(
            "Your order #%d has been completed successfully. Total: $%.2f. Thank you for your purchase!",
            event.getOrderId(),
            event.getTotal()
        );

        // Create notification entity
        Notification notification = new Notification();
        notification.setOrder(order);
        notification.setUser(user);
        notification.setNotificationType(NotificationType.ORDER_COMPLETED);
        notification.setMessage(message);
        notification.setEmailSent(false);

        // Save notification
        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", notification.getId());

        // Send fake email
        sendFakeEmail(notification, user.getEmail());

        // Update email sent flag
        notification.setEmailSent(true);
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Transactional
    public Notification createOrderExpiredNotification(OrderExpiredEvent event) {
        log.info("Creating notification for expired order {}", event.getOrderId());

        // Find order and user
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

        User user = userRepository.findById(event.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + event.getUserId()));

        // Build notification message
        String message = String.format(
            "Your order #%d has expired due to payment timeout. Please create a new order if you still wish to purchase.",
            event.getOrderId()
        );

        // Create notification entity
        Notification notification = new Notification();
        notification.setOrder(order);
        notification.setUser(user);
        notification.setNotificationType(NotificationType.ORDER_EXPIRED);
        notification.setMessage(message);
        notification.setEmailSent(false); // No email sent for expired orders

        // Save notification
        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {} (no email sent for expired orders)", notification.getId());

        return notification;
    }

    private void sendFakeEmail(Notification notification, String recipientEmail) {
        log.info("=========================================");
        log.info("SENDING EMAIL NOTIFICATION");
        log.info("To: {}", recipientEmail);
        log.info("Subject: Order Notification - Order #{}", notification.getOrder().getId());
        log.info("Body: {}", notification.getMessage());
        log.info("=========================================");

        // In a real implementation, this would use JavaMailSender:
        // mailSender.send(email);
    }
}
