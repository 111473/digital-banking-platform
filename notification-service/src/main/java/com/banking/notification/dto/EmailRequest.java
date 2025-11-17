package com.banking.notification.dto;

// Supporting classes

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class EmailRequest {
    private String to;
    private String subject;
    private String body;
}