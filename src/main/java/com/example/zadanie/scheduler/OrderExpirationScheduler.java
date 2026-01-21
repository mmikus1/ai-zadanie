package com.example.zadanie.scheduler;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.service.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void expireOldOrders() {
        log.info("Running order expiration job...");

        // Calculate cutoff time (10 minutes ago)
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(10);

        // Find PROCESSING orders older than 10 minutes
        List<Order> ordersToExpire = orderRepository.findByStatusAndCreatedAtBefore(
            OrderStatus.PROCESSING,
            cutoffTime
        );

        if (ordersToExpire.isEmpty()) {
            log.info("No orders to expire");
            return;
        }

        log.info("Found {} orders to expire", ordersToExpire.size());

        // Update each order to EXPIRED and publish event
        for (Order order : ordersToExpire) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // Publish OrderExpiredEvent
            orderEventProducer.publishOrderExpired(order);

            log.info("Expired order ID: {}", order.getId());
        }

        log.info("Order expiration job completed. Expired {} orders", ordersToExpire.size());
    }
}
