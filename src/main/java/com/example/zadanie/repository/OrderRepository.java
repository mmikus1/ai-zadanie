package com.example.zadanie.repository;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // JpaRepository provides:
    // - save(Order order)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - existsById(Long id)

    // Custom query methods
    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByProductId(Long productId);
}
