package com.banking.notification.dto;


@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CustomerContactInfo {
    private Integer customerId;
    private String email;
    private String mobileNumber;
}
