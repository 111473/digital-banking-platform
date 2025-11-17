package com.banking.customeraccount.controller;

import com.banking.customeraccount.dto.CustomerAccountResponse;
import com.banking.customeraccount.enums.KYCStatus;
import com.banking.customeraccount.service.CustomerAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for customer account management.
 * UPDATED: Added branch management endpoints
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerAccountController {

    private final CustomerAccountService service;

    public CustomerAccountController(CustomerAccountService service) {
        this.service = service;
    }

    /**
     * Get customer by customer ID
     * GET /api/customers/100001
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerAccountResponse> getCustomerByCustomerId(
            @PathVariable Integer customerId) {
        CustomerAccountResponse customer = service.getCustomerByCustomerId(customerId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get customer by application ID
     * GET /api/customers/by-application/100001
     */
    @GetMapping("/by-application/{applicationId}")
    public ResponseEntity<CustomerAccountResponse> getCustomerByApplicationId(
            @PathVariable Integer applicationId) {
        CustomerAccountResponse customer = service.getCustomerByApplicationId(applicationId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get all customers
     * GET /api/customers
     */
    @GetMapping
    public ResponseEntity<List<CustomerAccountResponse>> getAllCustomers() {
        List<CustomerAccountResponse> customers = service.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * ===== NEW: Get customers by branch =====
     * GET /api/customers/by-branch/BR001
     */
    @GetMapping("/by-branch/{branchCode}")
    public ResponseEntity<List<CustomerAccountResponse>> getCustomersByBranch(
            @PathVariable String branchCode) {
        List<CustomerAccountResponse> customers = service.getCustomersByBranch(branchCode);
        return ResponseEntity.ok(customers);
    }

    /**
     * Update customer contact information
     * PUT /api/customers/100001/contact
     * Body: {
     *   "phoneNumber": "+1234567890",
     *   "email": "newemail@example.com",
     *   "address": "New Address"
     * }
     */
    @PutMapping("/{customerId}/contact")
    public ResponseEntity<CustomerAccountResponse> updateCustomerContact(
            @PathVariable Integer customerId,
            @RequestBody Map<String, String> contactInfo) {
        CustomerAccountResponse updated = service.updateCustomerContact(
                customerId,
                contactInfo.get("phoneNumber"),
                contactInfo.get("email"),
                contactInfo.get("region"),
                contactInfo.get("province"),
                contactInfo.get("municipality"),
                contactInfo.get("street")
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * ===== NEW: Update customer branch assignment =====
     * PUT /api/customers/100001/branch
     * Body: {
     *   "branchCode": "BR002"
     * }
     */
    @PutMapping("/{customerId}/branch")
    public ResponseEntity<CustomerAccountResponse> updateCustomerBranch(
            @PathVariable Integer customerId,
            @RequestBody Map<String, String> request) {

        String branchCode = request.get("branchCode");
        if (branchCode == null || branchCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CustomerAccountResponse updated = service.updateCustomerBranch(customerId, branchCode);
        return ResponseEntity.ok(updated);
    }

    /**
     * Check if customer account exists for an application
     * GET /api/customers/exists/by-application/100001
     */
    @GetMapping("/exists/by-application/{applicationId}")
    public ResponseEntity<Map<String, Object>> checkCustomerExists(
            @PathVariable Integer applicationId) {
        boolean exists = service.customerAccountExists(applicationId);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("exists", exists);
        response.put("applicationId", applicationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update KYC status
     * PUT /api/customers/100001/kyc?status=VERIFIED
     */
    @PutMapping("/{customerId}/kyc")
    public ResponseEntity<CustomerAccountResponse> updateKycStatus(
            @PathVariable Integer customerId,
            @RequestParam KYCStatus status,
            @RequestParam(required = false) String verifiedBy) {

        CustomerAccountResponse updated = service.updateKycStatus(
                customerId,
                status,
                verifiedBy
        );
        return ResponseEntity.ok(updated);
    }
}