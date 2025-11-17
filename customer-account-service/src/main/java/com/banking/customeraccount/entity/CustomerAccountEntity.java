package com.banking.customeraccount.entity;

import com.banking.customeraccount.enums.AccountType;
import com.banking.customeraccount.enums.CurrencyType;
import com.banking.customeraccount.enums.IdentityType;
import com.banking.customeraccount.enums.KYCStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "customer_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountEntity {

    //id (Database Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Keep this

    @Column(name = "customer_id", unique = true, nullable = false)
    private Integer customerId;  // Database generates this

    @Column(name = "application_id", unique = true, nullable = false)
    private Integer applicationId;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 20)
    private IdentityType identityType;

    @NotBlank
    @Column(name = "id_Ref_Number", nullable = false)
    private String idRefNumber;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type", nullable = false, length = 5)
    private CurrencyType currencyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KYCStatus kycStatus;

    @Column(name = "kyc_verified_date")
    private LocalDateTime kycVerifiedDate;

    @Column(name = "kyc_next_review_date")
    private LocalDateTime kycNextReviewDate;

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @NotBlank
    @Column(name = "middle_name", nullable = false, length = 30)
    private String middleName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotBlank
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "region", nullable = false)
    private String region;

    @NotBlank
    @Column(name = "province", nullable = false)
    private String province;

    @NotBlank
    @Column(name = "municipality", nullable = false)
    private String municipality;

    @NotBlank
    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
