package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for transaction operations.
 *
 * NOTE: Transactions are also created AUTOMATICALLY via Kafka events
 * when bank accounts are created (initial deposit transaction).
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    /**
     * Process deposit
     *
     * POST /api/transactions/deposit
     * Body: {
     *   "accountNumber": 100001,
     *   "customerId": 100001,
     *   "amount": 500.00,
     *   "description": "Cash deposit",
     *   "referenceNumber": "DEP123"
     * }
     */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody Map<String, Object> request) {

        Integer accountNumber = (Integer) request.get("accountNumber");
        Integer customerId = (Integer) request.get("customerId");
        Double amount = ((Number) request.get("amount")).doubleValue();
        String description = (String) request.get("description");
        String referenceNumber = (String) request.get("referenceNumber");

        TransactionResponse response = service.processDeposit(
                accountNumber, customerId, amount, description, referenceNumber
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Process withdrawal
     *
     * POST /api/transactions/withdraw
     * Body: {
     *   "accountNumber": 100001,
     *   "customerId": 100001,
     *   "amount": 200.00,
     *   "description": "ATM withdrawal",
     *   "referenceNumber": "WD123"
     * }
     */
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody Map<String, Object> request) {

        Integer accountNumber = (Integer) request.get("accountNumber");
        Integer customerId = (Integer) request.get("customerId");
        Double amount = ((Number) request.get("amount")).doubleValue();
        String description = (String) request.get("description");
        String referenceNumber = (String) request.get("referenceNumber");

        TransactionResponse response = service.processWithdrawal(
                accountNumber, customerId, amount, description, referenceNumber
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get transaction history for an account
     *
     * GET /api/transactions/account/100001
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable Integer accountNumber) {
        List<TransactionResponse> transactions = service.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get account balance (calculated from transactions)
     *
     * GET /api/transactions/account/100001/balance
     */
    @GetMapping("/account/{accountNumber}/balance")
    public ResponseEntity<Map<String, Object>> getAccountBalance(
            @PathVariable Integer accountNumber) {
        Double balance = service.getAccountBalance(accountNumber);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("accountNumber", accountNumber);
        response.put("balance", balance);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Get single transaction
     *
     * GET /api/transactions/TXN-20251005143045-1234
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId) {
        TransactionResponse response = service.getTransaction(transactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions by date range
     *
     * GET /api/transactions/account/100001/range?start=2025-01-01T00:00:00&end=2025-12-31T23:59:59
     */
    @GetMapping("/account/{accountNumber}/range")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @PathVariable Integer accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<TransactionResponse> transactions =
                service.getTransactionsByDateRange(accountNumber, start, end);
        return ResponseEntity.ok(transactions);
    }
}