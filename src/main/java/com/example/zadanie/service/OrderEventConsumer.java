package com.example.zadanie.service;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.event.OrderCreatedEvent;
import com.example.zadanie.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "zadanie-order-group")
    @Transactional
    public void processOrderCreated(String message) {
        try {
            log.info("Received message from Kafka: {}", message);

            // Parse event
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            // Only process if status is PENDING (ignore PROCESSING events we republish)
            if (event.getStatus() != OrderStatus.PENDING) {
                log.info("Ignoring event for order {} with status {}", event.getOrderId(), event.getStatus());
                return;
            }

            log.info("Processing OrderCreatedEvent for order ID: {}", event.getOrderId());

            // 1. Find order
            Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

            // 2. Update status to PROCESSING
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
            log.info("Updated order {} status to PROCESSING", order.getId());

            // 3. Simulate payment processing delay (5 seconds)
            log.info("Simulating payment processing for order {}...", order.getId());
            Thread.sleep(5000);

            // 4. Random payment success (50% probability)
            boolean paymentSuccessful = ThreadLocalRandom.current().nextBoolean();

            if (paymentSuccessful) {
                // Payment successful - update to COMPLETED
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                log.info("Payment successful for order {}. Status updated to COMPLETED", order.getId());

                // Publish OrderCompletedEvent
                orderEventProducer.publishOrderCompleted(order);
            } else {
                // Payment failed - leave as PROCESSING (will expire via scheduled job)
                log.info("Payment failed for order {}. Status remains PROCESSING", order.getId());
            }

        } catch (InterruptedException e) {
            log.error("Payment processing interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent", e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }
}
