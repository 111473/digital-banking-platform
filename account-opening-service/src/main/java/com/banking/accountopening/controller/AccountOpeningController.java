package com.banking.accountopening.controller;

import com.banking.accountopening.dto.AccountOpeningRequest;
import com.banking.accountopening.dto.AccountOpeningResponse;
import com.banking.accountopening.enums.ApplicationStatus;
import com.banking.accountopening.enums.KYCStatus;
import com.banking.accountopening.service.AccountOpeningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for account opening applications.
 *
 * This controller handles the complete application workflow:
 * 1. Apply for account (PENDING)
 * 2. Submit application (SUBMITTED)
 * 3. Start review (UNDER_REVIEW)
 * 4. Update KYC status (VERIFIED/REJECTED)
 * 5. Approve or reject application (APPROVED/REJECTED)
 *
 * Base URL: /api/applications
 */
@RestController
@RequestMapping("/api/applications")
public class AccountOpeningController {

    private final AccountOpeningService service;

    public AccountOpeningController(AccountOpeningService service) {
        this.service = service;
    }

    /**
     * Apply for a new account
     *
     * POST /api/applications
     * Body: {
     *   "accountType": "SAVING_ACCOUNTS",
     *   "currencyType": "USD",
     *   "name": "John Doe",
     *   "phoneNumber": "+1234567890",
     *   "email": "john@example.com",
     *   "address": "123 Main St",
     *   "identityType": "PASSPORT",
     *   "idRefNumber": "P123456789"
     * }
     *
     * @param request Account opening request with customer details
     * @return Created application with PENDING status
     */
    @PostMapping
    public ResponseEntity<AccountOpeningResponse> applyForAccount(
            @Valid @RequestBody AccountOpeningRequest request) {
        AccountOpeningResponse response = service.applyForAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get application by ID
     *
     * GET /api/applications/100001
     *
     * @param applicationId The application ID
     * @return Application details
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<AccountOpeningResponse> getApplication(
            @PathVariable Integer applicationId) {
        AccountOpeningResponse response = service.getApplication(applicationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all applications (optionally filter by status)
     *
     * GET /api/applications
     * GET /api/applications?status=PENDING
     *
     * @param status Optional status filter
     * @return List of applications
     */
    @GetMapping
    public ResponseEntity<List<AccountOpeningResponse>> getAllApplications(
            @RequestParam(required = false) ApplicationStatus status) {
        if (status != null) {
            return ResponseEntity.ok(service.getApplicationsByStatus(status));
        }
        return ResponseEntity.ok(service.getAllApplications());
    }

    /**
     * Submit application for review
     *
     * PUT /api/applications/100001/submit
     *
     * Status change: PENDING → SUBMITTED
     *
     * @param applicationId The application ID to submit
     * @return Updated application
     */
    @PutMapping("/{applicationId}/submit")
    public ResponseEntity<AccountOpeningResponse> submitApplication(
            @PathVariable Integer applicationId) {
        AccountOpeningResponse response = service.submitApplication(applicationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Start review process
     *
     * PUT /api/applications/100001/start-review
     *
     * Status change: SUBMITTED → UNDER_REVIEW
     *
     * @param applicationId The application ID to review
     * @return Updated application
     */
    @PutMapping("/{applicationId}/start-review")
    public ResponseEntity<AccountOpeningResponse> startReview(
            @PathVariable Integer applicationId) {
        AccountOpeningResponse response = service.startReview(applicationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update KYC (Know Your Customer) status
     *
     * PUT /api/applications/100001/kyc?status=VERIFIED
     *
     * Valid KYC statuses: PENDING, VERIFIED, REJECTED
     *
     * @param applicationId The application ID
     * @param status New KYC status
     * @return Updated application
     */
    @PutMapping("/{applicationId}/kyc")
    public ResponseEntity<AccountOpeningResponse> updateKycStatus(
            @PathVariable Integer applicationId,
            @RequestParam KYCStatus status) {
        AccountOpeningResponse response = service.updateKycStatus(applicationId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve application
     *
     * PUT /api/applications/100001/approve
     *
     * Requirements:
     * - Status must be UNDER_REVIEW
     * - KYC must be VERIFIED
     *
     * Status change: UNDER_REVIEW → APPROVED
     *
     * After approval, customer account can be created via CustomerAccountController
     *
     * @param applicationId The application ID to approve
     * @return Updated application
     */
    @PutMapping("/{applicationId}/approve")
    public ResponseEntity<AccountOpeningResponse> approveApplication(
            @PathVariable Integer applicationId) {
        AccountOpeningResponse response = service.approveApplication(applicationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject application
     *
     * PUT /api/applications/100001/reject
     *
     * Requirements:
     * - Status must be UNDER_REVIEW
     * - KYC must be REJECTED
     *
     * Status change: UNDER_REVIEW → REJECTED
     *
     * @param applicationId The application ID to reject
     * @return Updated application
     */
    @PutMapping("/{applicationId}/reject")
    public ResponseEntity<AccountOpeningResponse> rejectApplication(
            @PathVariable Integer applicationId) {
        AccountOpeningResponse response = service.rejectApplication(applicationId);
        return ResponseEntity.ok(response);
    }
}