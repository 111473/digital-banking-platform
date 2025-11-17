package com.banking.notification.service;

import com.banking.notification.dto.EmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email Service - Sends emails using Spring Mail
 *
 * Supports:
 * - Simple text emails
 * - HTML emails
 * - Attachments (future enhancement)
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send simple text email
     */
    public void sendSimpleEmail(EmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bank.com");
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());

            mailSender.send(message);

            log.info("✅ [EMAIL_SENT] To: {} | Subject: {}", request.getTo(), request.getSubject());

        } catch (Exception e) {
            log.error("❌ [EMAIL_FAILED] To: {} | Error: {}", request.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send HTML email (recommended for better formatting)
     */
    public void sendEmail(EmailRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("noreply@bank.com");
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), false); // false = plain text, true = HTML

            mailSender.send(mimeMessage);

            log.info("✅ [EMAIL_SENT] To: {} | Subject: {}", request.getTo(), request.getSubject());

        } catch (MessagingException e) {
            log.error("❌ [EMAIL_FAILED] To: {} | Error: {}", request.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send HTML email with rich formatting
     */
    public void sendHtmlEmail(EmailRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("noreply@bank.com");
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());

            // Convert plain text to HTML
            String htmlBody = convertToHtml(request.getBody());
            helper.setText(htmlBody, true); // true = HTML format

            mailSender.send(mimeMessage);

            log.info("✅ [HTML_EMAIL_SENT] To: {} | Subject: {}", request.getTo(), request.getSubject());

        } catch (MessagingException e) {
            log.error("❌ [EMAIL_FAILED] To: {} | Error: {}", request.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    private String convertToHtml(String plainText) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #2c3e50; color: white; padding: 20px; text-align: center; }
                        .content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; }
                        .details { background: white; padding: 15px; margin: 15px 0; border-left: 4px solid #3498db; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🏦 Bank Account Notification</h2>
                        </div>
                        <div class="content">
                            <pre style="white-space: pre-wrap; font-family: Arial, sans-serif;">%s</pre>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply.</p>
                            <p>&copy; 2025 Your Bank. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(plainText);
    }
}

