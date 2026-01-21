package com.example.zadanie.service;

import com.example.zadanie.entity.Order;
import com.example.zadanie.event.OrderCompletedEvent;
import com.example.zadanie.event.OrderCreatedEvent;
import com.example.zadanie.event.OrderExpiredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "order-events";

    public void publishOrderCreated(Order order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotal(),
                order.getStatus(),
                LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, "order-created", json);
            log.info("Published OrderCreatedEvent for order ID: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCreatedEvent", e);
            throw new RuntimeException("Failed to publish order created event", e);
        }
    }

    public void publishOrderCompleted(Order order) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotal(),
                LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, "order-completed", json);
            log.info("Published OrderCompletedEvent for order ID: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCompletedEvent", e);
            throw new RuntimeException("Failed to publish order completed event", e);
        }
    }

    public void publishOrderExpired(Order order) {
        try {
            OrderExpiredEvent event = new OrderExpiredEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotal(),
                LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, "order-expired", json);
            log.info("Published OrderExpiredEvent for order ID: {}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderExpiredEvent", e);
            throw new RuntimeException("Failed to publish order expired event", e);
        }
    }
}
