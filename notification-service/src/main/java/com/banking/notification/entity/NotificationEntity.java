package com.banking.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Notification Entity - Audit trail for all notifications sent
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false)
    private String eventId; // From BankAccountCreatedEvent

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "account_number")
    private Integer accountNumber;

    @Column(name = "notification_type", nullable = false)
    private String notificationType; // ACCOUNT_CREATED, TRANSACTION_ALERT, etc.

    @Column(name = "email_sent")
    private Boolean emailSent;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "sms_sent")
    private Boolean smsSent;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;
}