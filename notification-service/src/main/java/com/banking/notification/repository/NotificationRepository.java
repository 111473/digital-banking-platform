package com.banking.notification.repository;

import com.banking.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Check if notification already sent (idempotency)
     */
    boolean existsByEventId(String eventId);

    /**
     * Find notifications by customer
     */
    List<NotificationEntity> findByCustomerId(Integer customerId);

    /**
     * Find notifications by type
     */
    List<NotificationEntity> findByNotificationType(String notificationType);

    /**
     * Find failed notifications for retry
     */
    List<NotificationEntity> findByEmailSentFalseAndRetryCountLessThan(int maxRetries);

    /**
     * Find notifications sent within date range
     */
    List<NotificationEntity> findBySentAtBetween(LocalDateTime start, LocalDateTime end);
}