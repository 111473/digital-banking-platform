package com.banking.notification.dto;

// Supporting classes

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class SmsRequest {
    private String to;
    private String message;
}