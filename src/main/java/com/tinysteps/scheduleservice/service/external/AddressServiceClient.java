package com.tinysteps.scheduleservice.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Client for communicating with address-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressServiceClient {

    private final WebClient publicWebClient;

    @Value("${services.address-service.url:http://ts-address-service}")
    private String addressServiceUrl;

    /**
     * Check if branch exists
     */
    public boolean branchExists(UUID branchId) {
        try {
            String url = addressServiceUrl + "/api/v1/addresses/branch/{branchId}/exists";

            Mono<Map> responseMono = publicWebClient.get()
                    .uri(url, branchId)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> response = responseMono.block();

            if (response != null) {
                return Boolean.TRUE.equals(response.get("exists"));
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking if branch {} exists: {}", branchId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get branch details
     */
    public Map<String, Object> getBranchDetails(UUID branchId) {
        try {
            String url = addressServiceUrl + "/api/v1/addresses/branch/{branchId}";

            Mono<Map> responseMono = publicWebClient.get()
                    .uri(url, branchId)
                    .retrieve()
                    .bodyToMono(Map.class);

            return responseMono.block();

        } catch (Exception e) {
            log.error("Error getting branch {} details: {}", branchId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate branch is active and operational
     */
    public boolean isBranchOperational(UUID branchId) {
        try {
            Map<String, Object> branchDetails = getBranchDetails(branchId);

            if (branchDetails != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) branchDetails.get("metadata");

                if (metadata != null) {
                    String status = (String) metadata.get("status");
                    return "ACTIVE".equalsIgnoreCase(status) || "OPERATIONAL".equalsIgnoreCase(status);
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking if branch {} is operational: {}", branchId, e.getMessage(), e);
            return false;
        }
    }
}