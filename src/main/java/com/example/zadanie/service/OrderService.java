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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderEventProducer orderEventProducer;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public Order createOrder(OrderCreateRequest request) {
        // 1. Validate user exists
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        // 2. Validate product exists
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + request.getProductId()));

        // 3. Validate stock availability
        if (product.getStock() <= 0) {
            throw new IllegalArgumentException("Product is out of stock");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStock() + ", Requested: " + request.getQuantity());
        }

        // 4. Calculate total price
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 5. Create order
        Order order = new Order();
        order.setUser(user);
        order.setProduct(product);
        order.setQuantity(request.getQuantity());
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING);
        // createdAt and updatedAt auto-populated by Hibernate

        // 6. Decrement product stock
        product.setStock(product.getStock() - request.getQuantity());
        productRepository.save(product);

        // 7. Save order
        Order savedOrder = orderRepository.save(order);

        // 8. Publish OrderCreatedEvent to Kafka
        orderEventProducer.publishOrderCreated(savedOrder);

        return savedOrder;
    }

    @Transactional
    public Order updateOrder(Long id, OrderUpdateRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        // Check if order can be updated (not completed or expired)
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot update order with status: " + order.getStatus());
        }

        // Update quantity if provided
        if (request.getQuantity() != null && !request.getQuantity().equals(order.getQuantity())) {
            // Recalculate total if quantity changed
            BigDecimal newTotal = order.getProduct().getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            order.setQuantity(request.getQuantity());
            order.setTotal(newTotal);
            // Note: Stock management for updates is complex - simplified here
            // In production, you'd need to adjust stock: restore old quantity, check new quantity
        }

        // Update status if provided
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        // updatedAt auto-updated by @UpdateTimestamp
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found with id: " + id);
        }
        // Note: In production, you might want to restore stock when deleting pending orders
        orderRepository.deleteById(id);
    }
}
