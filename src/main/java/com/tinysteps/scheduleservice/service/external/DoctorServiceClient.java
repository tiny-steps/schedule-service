package com.tinysteps.scheduleservice.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;

    @Value("${services.doctor-service.url:http://localhost:8083}")
    private String doctorServiceUrl;

    /**
     * Transfer doctor between branches
     */
    public boolean transferDoctorBetweenBranches(UUID doctorId, UUID sourceBranchId, UUID targetBranchId) {
        try {
            String url = doctorServiceUrl + "/api/v1/doctors/{doctorId}/transfer";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sourceBranchId", sourceBranchId);
            requestBody.put("targetBranchId", targetBranchId);
            requestBody.put("transferType", "BRANCH_TRANSFER");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    request, 
                    Map.class, 
                    doctorId
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully transferred doctor {} between branches", doctorId);
                return true;
            } else {
                log.warn("Doctor transfer failed with status: {}", response.getStatusCode());
                return false;
            }
            
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
            String url = doctorServiceUrl + "/api/v1/doctors/{doctorId}/availability?branchId={branchId}";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    url, 
                    Map.class, 
                    doctorId, 
                    branchId
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("available"));
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
            String url = doctorServiceUrl + "/api/v1/doctors/{doctorId}/branches";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, doctorId);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> branchIds = (java.util.List<String>) response.getBody().get("branchIds");
                
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