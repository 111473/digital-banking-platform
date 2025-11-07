package com.banking.branch.controller;

import com.banking.branch.dto.BranchRequest;
import com.banking.branch.dto.BranchResponse;
import com.banking.branch.enums.BranchStatus;
import com.banking.branch.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for branch management
 */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    /**
     * Create a new branch
     * POST /api/branches
     * Body: BranchRequest JSON
     */
    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody BranchRequest request) {
        BranchResponse created = service.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get branch by branch code
     * GET /api/branches/BR001
     */
    @GetMapping("/{branchCode}")
    public ResponseEntity<BranchResponse> getBranchByCode(
            @PathVariable String branchCode) {
        BranchResponse branch = service.getBranchByCode(branchCode);
        return ResponseEntity.ok(branch);
    }

    /**
     * Get all branches
     * GET /api/branches
     */
    @GetMapping
    public ResponseEntity<List<BranchResponse>> getAllBranches(
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(required = false) String region) {

        List<BranchResponse> branches;

        if (status != null) {
            branches = service.getBranchesByStatus(status);
        } else if (region != null) {
            branches = service.getBranchesByRegion(region);
        } else {
            branches = service.getAllBranches();
        }

        return ResponseEntity.ok(branches);
    }

    /**
     * Update branch information
     * PUT /api/branches/BR001
     * Body: BranchRequest JSON
     */
    @PutMapping("/{branchCode}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable String branchCode,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse updated = service.updateBranch(branchCode, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update branch status
     * PATCH /api/branches/BR001/status?status=ACTIVE
     */
    @PatchMapping("/{branchCode}/status")
    public ResponseEntity<BranchResponse> updateBranchStatus(
            @PathVariable String branchCode,
            @RequestParam BranchStatus status) {
        BranchResponse updated = service.updateBranchStatus(branchCode, status);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete/close a branch
     * DELETE /api/branches/BR001
     */
    @DeleteMapping("/{branchCode}")
    public ResponseEntity<Map<String, String>> deleteBranch(
            @PathVariable String branchCode) {
        service.deleteBranch(branchCode);
        return ResponseEntity.ok(Map.of(
                "message", "Branch closed successfully",
                "branchCode", branchCode
        ));
    }

    /**
     * Get customer count for a branch
     * GET /api/branches/BR001/customer-count
     */
    @GetMapping("/{branchCode}/customer-count")
    public ResponseEntity<Map<String, Object>> getCustomerCount(
            @PathVariable String branchCode) {
        Long count = service.getCustomerCount(branchCode);
        return ResponseEntity.ok(Map.of(
                "branchCode", branchCode,
                "customerCount", count
        ));
    }
}