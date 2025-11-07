package com.banking.customeraccount.client;

import com.banking.customeraccount.dto.BranchInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

/**
 * REST client for communicating with Branch Service
 * Fetches branch information for customer accounts
 */
@Slf4j
@Component
public class BranchServiceClient {

    private final RestTemplate restTemplate;
    private final String branchServiceUrl;

    public BranchServiceClient(
            RestTemplate restTemplate,
            @Value("${branch.service.url:http://branch-service:8083}") String branchServiceUrl) {
        this.restTemplate = restTemplate;
        this.branchServiceUrl = branchServiceUrl;
    }

    /**
     * Get branch information by branch code
     * @param branchCode the branch code
     * @return BranchInfoDTO or null if not found
     */
    public BranchInfoDTO getBranchInfo(String branchCode) {
        if (branchCode == null || branchCode.trim().isEmpty()) {
            log.warn("Attempted to fetch branch info with null/empty branch code");
            return null;
        }

        try {
            String url = branchServiceUrl + "/api/branches/" + branchCode;
            log.debug("Fetching branch info from: {}", url);

            BranchInfoDTO branchInfo = restTemplate.getForObject(url, BranchInfoDTO.class);
            log.info("Successfully fetched branch info for code: {}", branchCode);
            return branchInfo;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Branch not found with code: {}", branchCode);
            return null;

        } catch (Exception e) {
            log.error("Error fetching branch info for code: {}. Error: {}",
                    branchCode, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a branch exists and is active
     * @param branchCode the branch code
     * @return true if branch exists and is active
     */
    public boolean isBranchActive(String branchCode) {
        BranchInfoDTO branch = getBranchInfo(branchCode);
        return branch != null && "ACTIVE".equalsIgnoreCase(branch.getStatus());
    }

    /**
     * Validate branch code format
     * @param branchCode the branch code to validate
     * @return true if format is valid
     */
    public boolean isValidBranchCodeFormat(String branchCode) {
        return branchCode != null &&
                branchCode.matches("^BR\\d{3,6}$");
    }
}