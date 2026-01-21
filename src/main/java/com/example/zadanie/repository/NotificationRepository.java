package com.example.zadanie.repository;

import com.example.zadanie.entity.Notification;
import com.example.zadanie.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // JpaRepository provides:
    // - save(Notification notification)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)

    // Custom query methods
    List<Notification> findByUserId(Long userId);

    List<Notification> findByOrderId(Long orderId);

    List<Notification> findByNotificationType(NotificationType type);

    List<Notification> findByUserIdAndNotificationType(Long userId, NotificationType type);
}
