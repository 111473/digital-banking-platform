package com.banking.notification.service;

import com.banking.notification.dto.SmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

/**
 * SMS Service - Sends SMS notifications
 *
 * Providers supported:
 * - Twilio (recommended for international)
 * - Semaphore (for Philippines)
 * - AWS SNS SMS (if you prefer AWS)
 *
 * For development: Just logs SMS (mock mode)
 */
@Slf4j
@Service
public class SmsService {

    @Value("${sms.provider:mock}")
    private String smsProvider; // mock, twilio, semaphore

    @Value("${sms.api.key:}")
    private String apiKey;

    @Value("${sms.api.url:}")
    private String apiUrl;

    @Value("${sms.sender.name:Bank}")
    private String senderName;

    private final RestTemplate restTemplate;

    public SmsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendSms(SmsRequest request) {
        switch (smsProvider.toLowerCase()) {
            case "twilio":
                sendViaTwilio(request);
                break;
            case "semaphore":
                sendViaSemaphore(request);
                break;
            case "mock":
            default:
                sendMock(request);
                break;
        }
    }

    /**
     * Mock SMS (for development/testing)
     */
    private void sendMock(SmsRequest request) {
        log.info("📱 [SMS_MOCK] To: {} | Message: {}", request.getTo(), request.getMessage());
        log.info("✅ [SMS_SENT] Mock SMS sent successfully");
    }

    /**
     * Send SMS via Twilio
     * Docs: https://www.twilio.com/docs/sms/api
     */
    private void sendViaTwilio(SmsRequest request) {
        try {
            // Twilio API implementation
            String url = apiUrl + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(apiKey, System.getenv("TWILIO_AUTH_TOKEN"));

            String body = String.format("From=%s&To=%s&Body=%s",
                    senderName,
                    request.getTo(),
                    request.getMessage());

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("✅ [SMS_SENT] Twilio | To: {}", request.getTo());
            } else {
                log.error("❌ [SMS_FAILED] Twilio | Status: {}", response.getStatusCode());
                throw new RuntimeException("Twilio SMS failed");
            }

        } catch (Exception e) {
            log.error("❌ [SMS_ERROR] Twilio | To: {} | Error: {}", request.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send SMS via Twilio", e);
        }
    }

    /**
     * Send SMS via Semaphore (Philippine SMS provider)
     * Docs: https://semaphore.co/docs
     */
    private void sendViaSemaphore(SmsRequest request) {
        try {
            String url = String.format("%s?apikey=%s&number=%s&message=%s&sendername=%s",
                    apiUrl,
                    apiKey,
                    request.getTo(),
                    request.getMessage(),
                    senderName);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ [SMS_SENT] Semaphore | To: {}", request.getTo());
            } else {
                log.error("❌ [SMS_FAILED] Semaphore | Status: {}", response.getStatusCode());
                throw new RuntimeException("Semaphore SMS failed");
            }

        } catch (Exception e) {
            log.error("❌ [SMS_ERROR] Semaphore | To: {} | Error: {}", request.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send SMS via Semaphore", e);
        }
    }
}

