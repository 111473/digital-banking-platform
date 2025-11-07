package com.banking.bankaccount.controller;

import com.banking.bankaccount.dto.BankAccountResponse;
import com.banking.bankaccount.enums.AccountStatus;
import com.banking.bankaccount.service.BankAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for bank account operations.
 *
 * NOTE: Bank accounts are now created AUTOMATICALLY via Kafka events
 * when customer accounts are created in CustomerAccountService.
 *
 * Endpoints:
 * - GET    /api/bank-accounts/{accountNumber}           - Get account by number
 * - GET    /api/bank-accounts/customer/{customerId}     - Get all accounts for customer
 * - GET    /api/bank-accounts/branch/{branchCode}       - Get all accounts for branch (NEW)
 * - GET    /api/bank-accounts                           - Get all bank accounts
 * - PUT    /api/bank-accounts/{accountNumber}/status    - Update account status
 * - POST   /api/bank-accounts/{accountNumber}/deposit   - Deposit money
 * - POST   /api/bank-accounts/{accountNumber}/withdraw  - Withdraw money
 */
@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    /**
     * ⚠️ REMOVED: Manual bank account creation endpoint
     *
     * Bank accounts are now created automatically via Kafka events
     * when a customer account is created.
     *
     * Workflow:
     * 1. Customer account is created (CustomerAccountService)
     * 2. Kafka event is published automatically
     * 3. BankAccountService receives event and creates bank account
     * 4. Query bank account using GET /api/bank-accounts/customer/{customerId}
     */
    // @PostMapping - REMOVED

    /**
     * Get bank account by account number
     *
     * GET /api/bank-accounts/100001
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<BankAccountResponse> getBankAccount(
            @PathVariable Integer accountNumber) {
        BankAccountResponse response = service.getBankAccount(accountNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all bank accounts for a specific customer
     *
     * Use this endpoint to check if a bank account was created
     * after customer account creation.
     *
     * GET /api/bank-accounts/customer/100001
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BankAccountResponse>> getAccountsByCustomerId(
            @PathVariable Integer customerId) {
        List<BankAccountResponse> accounts = service.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * ✅ NEW: Get all bank accounts for a specific branch
     *
     * GET /api/bank-accounts/branch/BR001
     */
    @GetMapping("/branch/{branchCode}")
    public ResponseEntity<List<BankAccountResponse>> getAccountsByBranchCode(
            @PathVariable String branchCode) {
        List<BankAccountResponse> accounts = service.getAccountsByBranchCode(branchCode);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get all bank accounts in the system
     *
     * GET /api/bank-accounts
     */
    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> getAllBankAccounts() {
        List<BankAccountResponse> accounts = service.getAllBankAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Update account status (ACTIVE, SUSPENDED, FROZEN, CLOSED)
     *
     * PUT /api/bank-accounts/100001/status?status=SUSPENDED
     */
    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<BankAccountResponse> updateAccountStatus(
            @PathVariable Integer accountNumber,
            @RequestParam AccountStatus status) {
        BankAccountResponse response = service.updateAccountStatus(accountNumber, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Deposit money into account
     *
     * POST /api/bank-accounts/100001/deposit
     * Body: { "amount": 500.00 }
     */
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<BankAccountResponse> deposit(
            @PathVariable Integer accountNumber,
            @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BankAccountResponse response = service.deposit(accountNumber, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw money from account
     *
     * POST /api/bank-accounts/100001/withdraw
     * Body: { "amount": 200.00 }
     */
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<BankAccountResponse> withdraw(
            @PathVariable Integer accountNumber,
            @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BankAccountResponse response = service.withdraw(accountNumber, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if bank account exists for a customer
     *
     * Useful to verify if the Kafka event was processed successfully
     *
     * GET /api/bank-accounts/exists/customer/100001
     */
    @GetMapping("/exists/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> checkBankAccountExists(
            @PathVariable Integer customerId) {
        List<BankAccountResponse> accounts = service.getAccountsByCustomerId(customerId);
        boolean exists = !accounts.isEmpty();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("exists", exists);
        response.put("customerId", customerId);
        response.put("accountCount", accounts.size());

        return ResponseEntity.ok(response);
    }
}
