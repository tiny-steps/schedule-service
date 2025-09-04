package com.tinysteps.scheduleservice.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SecurityService {

    /**
     * Get the currently authenticated user's ID from JWT token claims
     * @return User ID from token claims
     * @throws RuntimeException if user is not authenticated or ID claim is missing
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getClaimAsString("id");
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("User ID not found in token claims");
            }
            return userId;
        }
        
        throw new RuntimeException("Invalid authentication token");
    }

    /**
     * Get the currently authenticated user's roles from JWT token claims
     * @return List of user roles
     */
    public List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsStringList("role");
        }
        
        throw new RuntimeException("Invalid authentication token");
    }

    /**
     * Check if the current user has a specific role
     * @param role Role to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        try {
            List<String> roles = getCurrentUserRoles();
            return roles != null && roles.contains(role);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the current user is an admin
     * @return true if user has ADMIN role, false otherwise
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if the current user can access resources for the given user ID
     * Users can access their own resources, admins can access any resources
     * @param targetUserId The user ID being accessed
     * @return true if access is allowed, false otherwise
     */
    public boolean canAccessUserResources(String targetUserId) {
        try {
            String currentUserId = getCurrentUserId();
            
            // Users can access their own resources
            if (currentUserId.equals(targetUserId)) {
                return true;
            }
            
            // Admins can access any resources
            return isAdmin();
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate that the current user can access resources for the given user ID
     * Throws exception if access is not allowed
     * @param targetUserId The user ID being accessed
     * @throws RuntimeException if access is not allowed
     */
    public void validateUserAccess(String targetUserId) {
        if (!canAccessUserResources(targetUserId)) {
            throw new RuntimeException("Access denied: You can only access your own resources");
        }
    }

    /**
     * Get the currently authenticated user's branch IDs from JWT token claims
     * @return List of branch IDs (as UUIDs) from token claims
     */
    public List<UUID> getBranchIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            List<String> branchIdStrings = jwt.getClaimAsStringList("branchIds");
            if (branchIdStrings == null || branchIdStrings.isEmpty()) {
                return List.of(); // Return empty list if no branch IDs
            }
            
            return branchIdStrings.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
        }
        
        throw new RuntimeException("Invalid authentication token");
    }

    /**
     * Get the currently authenticated user's primary branch ID from JWT token claims
     * @return Primary branch ID (as UUID) from token claims, or null if not set
     */
    public UUID getPrimaryBranchId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String primaryBranchIdString = jwt.getClaimAsString("primaryBranchId");
            if (primaryBranchIdString == null || primaryBranchIdString.isEmpty()) {
                return null; // Return null if no primary branch ID
            }
            
            return UUID.fromString(primaryBranchIdString);
        }
        
        throw new RuntimeException("Invalid authentication token");
    }

    /**
     * Check if the current user has access to a specific branch
     * @param branchId The branch ID to check access for
     * @return true if user has access to the branch, false otherwise
     */
    public boolean hasBranchAccess(String branchId) {
        try {
            UUID branchUuid = UUID.fromString(branchId);
            List<UUID> userBranchIds = getBranchIds();
            return userBranchIds.contains(branchUuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the current user has access to a specific branch
     * @param branchId The branch ID (as UUID) to check access for
     * @return true if user has access to the branch, false otherwise
     */
    public boolean hasBranchAccess(UUID branchId) {
        try {
            List<UUID> userBranchIds = getBranchIds();
            return userBranchIds.contains(branchId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate that the current user has access to a specific branch
     * Throws exception if access is not allowed
     * @param branchId The branch ID to validate access for
     * @throws RuntimeException if access is not allowed
     */
    public void validateBranchAccess(String branchId) {
        if (!hasBranchAccess(branchId)) {
            throw new RuntimeException("Access denied: You do not have access to this branch");
        }
    }

    /**
     * Validate that the current user has access to a specific branch
     * Throws exception if access is not allowed
     * @param branchId The branch ID (as UUID) to validate access for
     * @throws RuntimeException if access is not allowed
     */
    public void validateBranchAccess(UUID branchId) {
        if (!hasBranchAccess(branchId)) {
            throw new RuntimeException("Access denied: You do not have access to this branch");
        }
    }
}