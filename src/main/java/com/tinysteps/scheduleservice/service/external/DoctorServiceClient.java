package com.tinysteps.scheduleservice.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client for communicating with doctor-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorServiceClient {

    private final WebClient webClient;

    @Value("${services.doctor-service.url:http://localhost:8083}")
    private String doctorServiceUrl;

    /**
     * Transfer doctor between branches
     */
    public boolean transferDoctorBetweenBranches(UUID doctorId, UUID sourceBranchId, UUID targetBranchId) {
        try {
            String url = doctorServiceUrl + "/api/v1/doctors/" + doctorId + "/transfer";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sourceBranchId", sourceBranchId);
            requestBody.put("targetBranchId", targetBranchId);
            requestBody.put("transferType", "BRANCH_TRANSFER");
            
            Map response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("Successfully transferred doctor {} between branches", doctorId);
            return true;
            
        } catch (Exception e) {
            log.error("Error transferring doctor {} between branches: {}", doctorId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if doctor is available at target branch
     */
    public boolean isDoctorAvailableAtBranch(UUID doctorId, UUID branchId) {
        try {
            String url = doctorServiceUrl + "/api/v1/doctors/" + doctorId + "/availability?branchId=" + branchId;
            
            Map response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null) {
                return Boolean.TRUE.equals(response.get("available"));
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error checking doctor {} availability at branch {}: {}", 
                    doctorId, branchId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get doctor's current branches
     */
    public java.util.List<UUID> getDoctorBranches(UUID doctorId) {
        try {
            String url = doctorServiceUrl + "/api/v1/doctors/" + doctorId + "/branches";
            
            Map response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> branchIds = (java.util.List<String>) response.get("branchIds");
                
                if (branchIds != null) {
                    return branchIds.stream()
                            .map(UUID::fromString)
                            .collect(java.util.stream.Collectors.toList());
                }
            }
            
            return java.util.Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error getting doctor {} branches: {}", doctorId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}