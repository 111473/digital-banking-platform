package com.banking.notification.service;

import com.banking.events.BankAccountCreatedEvent;
import com.banking.notification.dto.CustomerContactInfo;
import com.banking.notification.dto.EmailRequest;
import com.banking.notification.dto.SmsRequest;
import com.banking.notification.entity.NotificationEntity;
import com.banking.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Notification Service - Listens to bank account events and sends notifications
 *
 * Flow:
 * 1. Bank Account Service publishes BankAccountCreatedEvent to Kafka
 * 2. Notification Service consumes event
 * 3. Sends email/SMS to customer
 * 4. Logs notification in database for audit
 */
@Slf4j
@Service
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationRepository notificationRepository;

    public NotificationService(EmailService emailService,
                               SmsService smsService,
                               NotificationRepository notificationRepository) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Listen to bank-account-created events and send notifications
     */
    @KafkaListener(
            topics = "bank-account-created",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBankAccountCreated(
            @Payload BankAccountCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("📨 [RECEIVED] BankAccountCreatedEvent | AccountNumber: {} | CustomerId: {} | Partition: {} | Offset: {}",
                event.getAccountNumber(), event.getCustomerId(), partition, offset);

        try {
            // Step 1: Check if notification already sent (idempotency)
            if (notificationRepository.existsByEventId(event.getEventId())) {
                log.warn("⚠️ [DUPLICATE] Notification already sent | EventId: {} | Skipping",
                        event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Step 2: Fetch customer contact details (from Customer Service or database)
            CustomerContactInfo contactInfo = fetchCustomerContactInfo(event.getCustomerId());

            // Step 3: Send Email Notification
            boolean emailSent = sendEmailNotification(event, contactInfo);

            // Step 4: Send SMS Notification (optional)
            boolean smsSent = sendSmsNotification(event, contactInfo);

            // Step 5: Save notification record for audit
            saveNotificationRecord(event, emailSent, smsSent, contactInfo);

            // Step 6: Acknowledge message
            acknowledgment.acknowledge();

            log.info("✅ [SENT] Notifications | AccountNumber: {} | Email: {} | SMS: {} | Offset: {}",
                    event.getAccountNumber(), emailSent, smsSent, offset);

        } catch (Exception e) {
            log.error("❌ [ERROR] Failed to send notification | AccountNumber: {} | Error: {}",
                    event.getAccountNumber(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Notification sending failed", e);
        }
    }

    private boolean sendEmailNotification(BankAccountCreatedEvent event, CustomerContactInfo contactInfo) {
        try {
            if (contactInfo.getEmail() == null || contactInfo.getEmail().isEmpty()) {
                log.warn("⚠️ No email found for customer: {}", event.getCustomerId());
                return false;
            }

            EmailRequest emailRequest = EmailRequest.builder()
                    .to(contactInfo.getEmail())
                    .subject("Welcome! Your Bank Account is Ready")
                    .body(buildEmailBody(event, contactInfo))
                    .build();

            emailService.sendEmail(emailRequest);
            return true;

        } catch (Exception e) {
            log.error("❌ Email sending failed | CustomerId: {} | Error: {}",
                    event.getCustomerId(), e.getMessage());
            return false;
        }
    }

    private boolean sendSmsNotification(BankAccountCreatedEvent event, CustomerContactInfo contactInfo) {
        try {
            if (contactInfo.getMobileNumber() == null || contactInfo.getMobileNumber().isEmpty()) {
                log.warn("⚠️ No mobile number found for customer: {}", event.getCustomerId());
                return false;
            }

            String smsMessage = String.format(
                    "Welcome %s! Your %s account #%d is now active with balance: ₱%.2f. Thank you for banking with us!",
                    event.getFirstName(),
                    event.getMiddleName(),
                    event.getLastName(),
                    event.getAccountType(),
                    event.getAccountNumber(),
                    event.getInitialBalance()
            );

            SmsRequest smsRequest = SmsRequest.builder()
                    .to(contactInfo.getMobileNumber())
                    .message(smsMessage)
                    .build();

            smsService.sendSms(smsRequest);
            return true;

        } catch (Exception e) {
            log.error("❌ SMS sending failed | CustomerId: {} | Error: {}",
                    event.getCustomerId(), e.getMessage());
            return false;
        }
    }

    private String buildEmailBody(BankAccountCreatedEvent event, CustomerContactInfo contactInfo) {
        return String.format("""
                Dear %s,
                
                Congratulations! Your bank account has been successfully created.
                
                Account Details:
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Account Number:  %d
                Account Type:    %s
                Branch Code:     %s
                Initial Balance: ₱%.2f
                Interest Rate:   %.2f%%
                Status:          %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                You can now start using your account for deposits, withdrawals, and transfers.
                
                If you have any questions, please contact us at support@bank.com or visit your nearest branch.
                
                Thank you for choosing our bank!
                
                Best regards,
                The Banking Team
                """,
                event.getFirstName(),
                event.getMiddleName(),
                event.getLastName(),
                event.getAccountNumber(),
                event.getAccountType(),
                event.getBranchCode(),
                event.getInitialBalance(),
                event.getInterestRate(),
                event.getAccountStatus()
        );
    }

    private CustomerContactInfo fetchCustomerContactInfo(Integer customerId) {
        // Option 1: Call Customer Service via Feign Client
        // return customerServiceClient.getCustomerContactInfo(customerId);

        // Option 2: Store contact info in local database (event sourcing pattern)
        // return contactInfoRepository.findByCustomerId(customerId);

        // For now, return mock data
        return CustomerContactInfo.builder()
                .customerId(customerId)
                .email("customer" + customerId + "@email.com")
                .mobileNumber("+639171234567")
                .build();
    }

    private void saveNotificationRecord(BankAccountCreatedEvent event,
                                        boolean emailSent,
                                        boolean smsSent,
                                        CustomerContactInfo contactInfo) {
        NotificationEntity notification = NotificationEntity.builder()
                .eventId(event.getEventId())
                .customerId(event.getCustomerId())
                .accountNumber(event.getAccountNumber())
                .notificationType("ACCOUNT_CREATED")
                .emailSent(emailSent)
                .emailAddress(contactInfo.getEmail())
                .smsSent(smsSent)
                .mobileNumber(contactInfo.getMobileNumber())
                .sentAt(java.time.LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }
}