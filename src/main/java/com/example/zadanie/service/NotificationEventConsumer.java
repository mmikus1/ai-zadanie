package com.example.zadanie.service;

import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "order-events",
        groupId = "zadanie-notification-group"
    )
    @Transactional
    public void consumeOrderEvents(ConsumerRecord<String, String> record) {
        try {
            String key = record.key();
            String message = record.value();

            log.info("Notification consumer received event with key: {}", key);

            if ("order-completed".equals(key)) {
                // Deserialize to OrderCompletedEvent
                OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);
                log.info("Processing OrderCompletedEvent for order ID: {}", event.getOrderId());

                // Create notification (includes email sending)
                notificationService.createOrderCompletedNotification(event);
                log.info("Created notification and sent email for completed order {}", event.getOrderId());

            } else if ("order-expired".equals(key)) {
                // Deserialize to OrderExpiredEvent
                OrderExpiredEvent event = objectMapper.readValue(message, OrderExpiredEvent.class);
                log.info("Processing OrderExpiredEvent for order ID: {}", event.getOrderId());

                // Create notification (no email)
                notificationService.createOrderExpiredNotification(event);
                log.info("Created notification for expired order {}", event.getOrderId());

            } else {
                // Ignore other event types (order-created)
                log.debug("Ignoring event with key: {}", key);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event", e);
            throw new RuntimeException("Failed to process notification event", e);
        } catch (Exception e) {
            log.error("Failed to create notification", e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }
}
