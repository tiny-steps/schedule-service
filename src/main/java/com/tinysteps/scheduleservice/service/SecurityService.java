package com.tinysteps.scheduleservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced SecurityService with comprehensive defensive programming patterns.
 * Provides secure access to JWT token claims with robust input validation,
 * comprehensive error handling, and detailed logging for security operations.
 * 
 * Features:
 * - Input validation for all parameters
 * - Comprehensive authentication and authorization checks
 * - UUID format validation
 * - Detailed security logging
 * - Fail-safe error handling
 * - Domain type validation
 */
@Service
@Slf4j
public class SecurityService {
    
    private static final Set<String> VALID_DOMAIN_TYPES = Set.of(
        "healthcare", "ecommerce", "cab-booking"
    );

    /**
     * Get the currently authenticated user's ID from JWT token with comprehensive validation
     * @return User ID from token claims
     * @throws SecurityException if user is not authenticated or token is invalid
     */
    public String getCurrentUserId() {
        log.debug("Retrieving current user ID from JWT token");
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.warn("No authentication context found");
                throw new SecurityException("User is not authenticated - no authentication context");
            }
            
            if (!authentication.isAuthenticated()) {
                log.warn("User is not authenticated - authentication failed");
                throw new SecurityException("User is not authenticated - authentication failed");
            }
            
            if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
                log.warn("Invalid authentication token type: {}", 
                    authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
                throw new SecurityException("Invalid authentication token type");
            }
            
            String userId = jwt.getSubject();
            if (!StringUtils.hasText(userId)) {
                log.warn("JWT token missing or empty subject claim");
                throw new SecurityException("JWT token missing user ID");
            }
            
            // Validate UUID format
            try {
                UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format for user ID: {}", userId);
                throw new SecurityException("Invalid user ID format");
            }
            
            log.debug("Successfully retrieved user ID: {}", userId);
            return userId;
            
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument while retrieving user ID", e);
            throw new SecurityException("Invalid token format");
        } catch (Exception e) {
            log.error("Unexpected error while retrieving user ID", e);
            throw new SecurityException("Authentication error");
        }
    }

    /**
     * Get the currently authenticated user's roles from JWT token with comprehensive validation
     * @return List of user roles from token claims (never null, may be empty)
     */
    public List<String> getCurrentUserRoles() {
        log.debug("Retrieving current user roles from JWT token");
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.warn("No authentication context found while retrieving roles");
                throw new SecurityException("User is not authenticated - no authentication context");
            }
            
            if (!authentication.isAuthenticated()) {
                log.warn("User is not authenticated while retrieving roles");
                throw new SecurityException("User is not authenticated - authentication failed");
            }
            
            if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
                log.warn("Invalid authentication token type while retrieving roles: {}", 
                    authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
                throw new SecurityException("Invalid authentication token type");
            }
            
            List<String> roles = jwt.getClaimAsStringList("role");
            if (roles == null) {
                log.debug("No roles claim found in JWT token, returning empty list");
                return List.of();
            }
            
            // Filter out null or empty roles
            List<String> validRoles = roles.stream()
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
            
            log.debug("Successfully retrieved {} roles for user", validRoles.size());
            return validRoles;
            
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while retrieving user roles", e);
            throw new SecurityException("Error retrieving user roles");
        }
    }

    /**
     * Check if the current user has a specific role with comprehensive validation
     * @param role The role to check for
     * @return true if user has the role, false otherwise (fail-safe)
     */
    public boolean hasRole(String role) {
        if (!StringUtils.hasText(role)) {
            log.warn("Role check attempted with null or empty role");
            return false;
        }
        
        log.debug("Checking if current user has role: {}", role);
        
        try {
            List<String> userRoles = getCurrentUserRoles();
            boolean hasRole = userRoles.contains(role);
            log.debug("Role check result for '{}': {}", role, hasRole);
            return hasRole;
        } catch (SecurityException e) {
            log.warn("Security exception during role check for '{}': {}", role, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during role check for '{}'", role, e);
            return false;
        }
    }

    /**
     * Check if the current user is an admin with fail-safe handling
     * @return true if user is admin, false otherwise (fail-safe)
     */
    public boolean isAdmin() {
        log.debug("Checking if current user is admin");
        try {
            boolean isAdmin = hasRole("ADMIN");
            log.debug("Admin check result: {}", isAdmin);
            return isAdmin;
        } catch (Exception e) {
            log.error("Error checking admin status", e);
            return false;
        }
    }

    /**
     * Check if the current user can access resources for a specific user with comprehensive validation
     * Users can access their own resources, admins can access any resources
     * @param targetUserId The user ID being accessed
     * @return true if access is allowed, false otherwise (fail-safe)
     */
    public boolean canAccessUserResources(String targetUserId) {
        if (!StringUtils.hasText(targetUserId)) {
            log.warn("User access check attempted with null or empty target user ID");
            return false;
        }
        
        // Validate UUID format
        try {
            UUID.fromString(targetUserId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for target user ID: {}", targetUserId);
            return false;
        }
        
        log.debug("Checking user access for target user: {}", targetUserId);
        
        try {
            String currentUserId = getCurrentUserId();
            
            // Users can access their own resources
            if (currentUserId.equals(targetUserId)) {
                log.debug("User granted access to own resources: {}", targetUserId);
                return true;
            }
            
            // Admins can access any resources
            boolean isAdmin = isAdmin();
            if (isAdmin) {
                log.debug("Admin user granted access to target user: {}", targetUserId);
                return true;
            }
            
            log.debug("User access denied for '{}' accessing '{}'", currentUserId, targetUserId);
            return false;
            
        } catch (SecurityException e) {
            log.warn("Security exception during user access check for '{}': {}", targetUserId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during user access check for '{}'", targetUserId, e);
            return false;
        }
    }

    /**
     * Validate that the current user can access resources for the given user ID with comprehensive validation
     * Throws exception if access is not allowed
     * @param targetUserId The user ID being accessed
     * @throws SecurityException if access is not allowed or validation fails
     */
    public void validateUserAccess(String targetUserId) {
        log.debug("Validating user access for target user: {}", targetUserId);
        
        try {
            if (!canAccessUserResources(targetUserId)) {
                String currentUserId;
                try {
                    currentUserId = getCurrentUserId();
                } catch (Exception e) {
                    currentUserId = "unknown";
                }
                log.warn("Access denied: User '{}' attempted to access resources for '{}'", currentUserId, targetUserId);
                throw new SecurityException("Access denied: You can only access your own resources");
            }
            log.debug("User access validation successful for target user: {}", targetUserId);
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during user access validation for '{}'", targetUserId, e);
            throw new SecurityException("Access validation failed");
        }
    }

    /**
     * Get the currently authenticated user's context IDs for a specific domain from JWT token claims with comprehensive validation
     * @param domainType The domain type (e.g., "healthcare", "ecommerce", "cab-booking")
     * @return List of context IDs (as UUIDs) from token claims (never null, may be empty)
     * @throws SecurityException if user is not authenticated or domain type is invalid
     */
    public List<UUID> getContextIds(String domainType) {
        if (!StringUtils.hasText(domainType)) {
            log.warn("Context IDs requested with null or empty domain type");
            throw new SecurityException("Domain type cannot be null or empty");
        }
        
        if (!VALID_DOMAIN_TYPES.contains(domainType)) {
            log.warn("Context IDs requested with invalid domain type: {}", domainType);
            throw new SecurityException("Invalid domain type: " + domainType);
        }
        
        log.debug("Retrieving context IDs for domain: {}", domainType);
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.warn("No authentication context found while retrieving context IDs for domain: {}", domainType);
                throw new SecurityException("User is not authenticated - no authentication context");
            }
            
            if (!authentication.isAuthenticated()) {
                log.warn("User is not authenticated while retrieving context IDs for domain: {}", domainType);
                throw new SecurityException("User is not authenticated - authentication failed");
            }
            
            if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
                log.warn("Invalid authentication token type while retrieving context IDs for domain: {}", domainType);
                throw new SecurityException("Invalid authentication token type");
            }
            
            // Try new domain-agnostic context claims first
            String contextClaimKey = domainType + "_context_ids";
            List<String> contextIdStrings = jwt.getClaimAsStringList(contextClaimKey);
            
            if (contextIdStrings != null && !contextIdStrings.isEmpty()) {
                List<UUID> contextIds = contextIdStrings.stream()
                    .filter(Objects::nonNull)
                    .filter(StringUtils::hasText)
                    .map(idString -> {
                        try {
                            return UUID.fromString(idString);
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid UUID format in context IDs for domain '{}': {}", domainType, idString);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                
                log.debug("Successfully retrieved {} context IDs for domain: {}", contextIds.size(), domainType);
                return contextIds;
            }
            
            // Fall back to legacy branch claims for healthcare domain
            if ("healthcare".equals(domainType)) {
                List<String> branchIdStrings = jwt.getClaimAsStringList("branchIds");
                if (branchIdStrings != null && !branchIdStrings.isEmpty()) {
                    List<UUID> branchIds = branchIdStrings.stream()
                        .filter(Objects::nonNull)
                        .filter(StringUtils::hasText)
                        .map(idString -> {
                            try {
                                return UUID.fromString(idString);
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid UUID format in legacy branch IDs: {}", idString);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    
                    log.debug("Successfully retrieved {} legacy branch IDs for healthcare domain", branchIds.size());
                    return branchIds;
                }
            }
            
            log.debug("No context IDs found for domain: {}, returning empty list", domainType);
            return List.of(); // Return empty list if no context IDs found
            
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while retrieving context IDs for domain: {}", domainType, e);
            throw new SecurityException("Error retrieving context IDs");
        }
    }

    /**
     * Get the currently authenticated user's primary context ID for a specific domain from JWT token claims with comprehensive validation
     * @param domainType The domain type (e.g., "healthcare", "ecommerce", "cab-booking")
     * @return Primary context ID (as UUID) from token claims, or null if not set
     * @throws SecurityException if user is not authenticated or domain type is invalid
     */
    public UUID getPrimaryContextId(String domainType) {
        if (!StringUtils.hasText(domainType)) {
            log.warn("Primary context ID requested with null or empty domain type");
            throw new SecurityException("Domain type cannot be null or empty");
        }
        
        if (!VALID_DOMAIN_TYPES.contains(domainType)) {
            log.warn("Primary context ID requested with invalid domain type: {}", domainType);
            throw new SecurityException("Invalid domain type: " + domainType);
        }
        
        log.debug("Retrieving primary context ID for domain: {}", domainType);
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.warn("No authentication context found while retrieving primary context ID for domain: {}", domainType);
                throw new SecurityException("User is not authenticated - no authentication context");
            }
            
            if (!authentication.isAuthenticated()) {
                log.warn("User is not authenticated while retrieving primary context ID for domain: {}", domainType);
                throw new SecurityException("User is not authenticated - authentication failed");
            }
            
            if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
                log.warn("Invalid authentication token type while retrieving primary context ID for domain: {}", domainType);
                throw new SecurityException("Invalid authentication token type");
            }
            
            // Try new domain-agnostic context claims first
            String primaryContextClaimKey = domainType + "_primary_context_id";
            String primaryContextIdString = jwt.getClaimAsString(primaryContextClaimKey);
            
            if (StringUtils.hasText(primaryContextIdString)) {
                try {
                    UUID primaryContextId = UUID.fromString(primaryContextIdString);
                    log.debug("Successfully retrieved primary context ID for domain '{}': {}", domainType, primaryContextId);
                    return primaryContextId;
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID format for primary context ID in domain '{}': {}", domainType, primaryContextIdString);
                    // Continue to fallback options
                }
            }
            
            // Fall back to legacy branch claims for healthcare domain
            if ("healthcare".equals(domainType)) {
                String primaryBranchIdString = jwt.getClaimAsString("primary_branch_id");
                if (!StringUtils.hasText(primaryBranchIdString)) {
                    // Try legacy claim name
                    primaryBranchIdString = jwt.getClaimAsString("primaryBranchId");
                }
                
                if (StringUtils.hasText(primaryBranchIdString)) {
                    try {
                        UUID primaryBranchId = UUID.fromString(primaryBranchIdString);
                        log.debug("Successfully retrieved legacy primary branch ID for healthcare domain: {}", primaryBranchId);
                        return primaryBranchId;
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID format for legacy primary branch ID: {}", primaryBranchIdString);
                    }
                }
            }
            
            log.debug("No primary context ID found for domain: {}", domainType);
            return null; // Return null if no primary context ID found
            
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while retrieving primary context ID for domain: {}", domainType, e);
            throw new SecurityException("Error retrieving primary context ID");
        }
    }

    /**
     * Get the currently authenticated user's branch IDs from JWT token claims
     * @deprecated Use getContextIds("healthcare") instead
     * @return List of branch IDs (as UUIDs) from token claims
     */
    @Deprecated
    public List<UUID> getBranchIds() {
        // Use new context-based method with fallback to legacy claims
        return getContextIds("healthcare");
    }

    /**
     * Get the currently authenticated user's primary branch ID from JWT token claims
     * @deprecated Use getPrimaryContextId("healthcare") instead
     * @return Primary branch ID (as UUID) from token claims, or null if not set
     */
    @Deprecated
    public UUID getPrimaryBranchId() {
        // Use new context-based method with fallback to legacy claims
        return getPrimaryContextId("healthcare");
    }

    /**
     * Check if the current user has access to a specific context in a domain with comprehensive validation
     * @param contextId The context ID to check access for (as String)
     * @param domainType The domain type (e.g., "healthcare", "ecommerce", "cab-booking")
     * @return true if user has access, false otherwise
     */
    public boolean hasAccessToContext(String contextId, String domainType) {
        if (!StringUtils.hasText(contextId)) {
            log.warn("Context access check requested with null or empty contextId");
            return false;
        }
        
        if (!StringUtils.hasText(domainType)) {
            log.warn("Context access check requested with null or empty domain type for contextId: {}", contextId);
            return false;
        }
        
        if (!VALID_DOMAIN_TYPES.contains(domainType)) {
            log.warn("Context access check requested with invalid domain type '{}' for contextId: {}", domainType, contextId);
            return false;
        }
        
        log.debug("Checking context access for contextId: {} in domain: {}", contextId, domainType);
        
        try {
            // Admin users have access to all contexts
            if (isAdmin()) {
                log.debug("Admin user has access to context: {} in domain: {}", contextId, domainType);
                return true;
            }
            
            UUID contextUuid;
            try {
                contextUuid = UUID.fromString(contextId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format for contextId: {}", contextId);
                return false;
            }
            
            return hasAccessToContext(contextUuid, domainType);
        } catch (SecurityException e) {
            log.warn("Security exception while checking context access for contextId: {} in domain: {}", contextId, domainType, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking context access for contextId: {} in domain: {}", contextId, domainType, e);
            return false;
        }
    }

    /**
     * Check if the current user has access to a specific context in a domain with comprehensive validation
     * @param contextId The context ID to check access for (as UUID)
     * @param domainType The domain type (e.g., "healthcare", "ecommerce", "cab-booking")
     * @return true if user has access, false otherwise
     */
    public boolean hasAccessToContext(UUID contextId, String domainType) {
        if (contextId == null) {
            log.warn("Context access check requested with null contextId");
            return false;
        }
        
        if (!StringUtils.hasText(domainType)) {
            log.warn("Context access check requested with null or empty domain type for contextId: {}", contextId);
            return false;
        }
        
        if (!VALID_DOMAIN_TYPES.contains(domainType)) {
            log.warn("Context access check requested with invalid domain type '{}' for contextId: {}", domainType, contextId);
            return false;
        }
        
        log.debug("Checking context access for contextId: {} in domain: {}", contextId, domainType);
        
        try {
            // Admin users have access to all contexts
            if (isAdmin()) {
                log.debug("Admin user has access to context: {} in domain: {}", contextId, domainType);
                return true;
            }
            
            List<UUID> userContextIds = getContextIds(domainType);
            boolean hasAccess = userContextIds.contains(contextId);
            
            if (hasAccess) {
                log.debug("User has access to context: {} in domain: {}", contextId, domainType);
            } else {
                log.debug("User does not have access to context: {} in domain: {}", contextId, domainType);
            }
            
            return hasAccess;
        } catch (SecurityException e) {
            log.warn("Security exception while checking context access for contextId: {} in domain: {}", contextId, domainType, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking context access for contextId: {} in domain: {}", contextId, domainType, e);
            return false;
        }
    }

    /**
     * Validate that the current user has access to a specific context in a domain
     * Throws exception if access is denied
     * @param contextId The context ID to validate access for (as String)
     * @param domainType The domain type (e.g., "healthcare", "ecommerce", "cab-booking")
     * @throws SecurityException if user doesn't have access to the context or validation fails
     */
    public void validateContextAccess(String contextId, String domainType) {
        if (contextId == null) {
            log.warn("Context access validation requested with null contextId");
            throw new SecurityException("Context ID cannot be null");
        }
        
        if (!StringUtils.hasText(domainType)) {
            log.warn("Context access validation requested with null or empty domain type for contextId: {}", contextId);
            throw new SecurityException("Domain type cannot be null or empty");
        }
        
        if (!VALID_DOMAIN_TYPES.contains(domainType)) {
            log.warn("Context access validation requested with invalid domain type '{}' for contextId: {}", domainType, contextId);
            throw new SecurityException("Invalid domain type: " + domainType);
        }
        
        log.debug("Validating context access for contextId: {} in domain: {}", contextId, domainType);
        
        try {
            if (!hasAccessToContext(contextId, domainType)) {
                String userId = "unknown";
                try {
                    userId = getCurrentUserId();
                } catch (Exception e) {
                    // Ignore error in getting user ID for logging
                }
                log.warn("Access denied to context '{}' in domain '{}' for user: {}", contextId, domainType, userId);
                throw new SecurityException("Access denied: User does not have access to context " + contextId + " in domain " + domainType);
            }
            
            log.debug("Context access validated successfully for contextId: {} in domain: {}", contextId, domainType);
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during context access validation for contextId: {} in domain: {}", contextId, domainType, e);
            throw new SecurityException("Error validating context access");
        }
    }

    /**
     * Validate that the current user has access to a specific context in a domain
     * Throws exception if access is denied
     * @param contextId The context ID to validate access for (as UUID)
     * @param domainType The domain type (e.g., "healthcare", "ecommerce", "cab-booking")
     * @throws SecurityException if user doesn't have access to the context or validation fails
     */
    public void validateContextAccess(UUID contextId, String domainType) {
        if (contextId == null) {
            log.warn("Context access validation requested with null contextId");
            throw new SecurityException("Context ID cannot be null");
        }
        
        if (!StringUtils.hasText(domainType)) {
            log.warn("Context access validation requested with null or empty domain type for contextId: {}", contextId);
            throw new SecurityException("Domain type cannot be null or empty");
        }
        
        if (!VALID_DOMAIN_TYPES.contains(domainType)) {
            log.warn("Context access validation requested with invalid domain type '{}' for contextId: {}", domainType, contextId);
            throw new SecurityException("Invalid domain type: " + domainType);
        }
        
        log.debug("Validating context access for contextId: {} in domain: {}", contextId, domainType);
        
        try {
            if (!hasAccessToContext(contextId, domainType)) {
                String userId = "unknown";
                try {
                    userId = getCurrentUserId();
                } catch (Exception e) {
                    // Ignore error in getting user ID for logging
                }
                log.warn("Access denied to context '{}' in domain '{}' for user: {}", contextId, domainType, userId);
                throw new SecurityException("Access denied: User does not have access to context " + contextId + " in domain " + domainType);
            }
            
            log.debug("Context access validated successfully for contextId: {} in domain: {}", contextId, domainType);
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during context access validation for contextId: {} in domain: {}", contextId, domainType, e);
            throw new SecurityException("Error validating context access");
        }
    }

    /**
     * Check if the current user has access to a specific branch
     * @deprecated Use hasAccessToContext(branchId, "healthcare") instead
     * @param branchId The branch ID to check access for
     * @return true if user has access to the branch, false otherwise
     */
    @Deprecated
    public boolean hasBranchAccess(String branchId) {
        if (branchId == null || branchId.isEmpty()) {
            return false;
        }
        
        try {
            UUID branchUuid = UUID.fromString(branchId);
            return hasBranchAccess(branchUuid);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if the current user has access to a specific branch
     * @deprecated Use hasAccessToContext(branchId, "healthcare") instead
     * @param branchId The branch ID (as UUID) to check access for
     * @return true if user has access to the branch, false otherwise
     */
    @Deprecated
    public boolean hasBranchAccess(UUID branchId) {
        if (branchId == null) {
            return false;
        }
        
        // Try new context-based access first
        try {
            return hasAccessToContext(branchId, "healthcare");
        } catch (Exception e) {
            // Fall back to legacy branch access
        }
        
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
     * @deprecated Use validateContextAccess(branchId, "healthcare") instead
     * @param branchId The branch ID to validate access for
     * @throws RuntimeException if access is not allowed
     */
    @Deprecated
    public void validateBranchAccess(String branchId) {
        if (!hasBranchAccess(branchId)) {
            throw new RuntimeException("Access denied: You do not have access to this branch");
        }
    }

    /**
     * Validate that the current user has access to a specific branch
     * Throws exception if access is not allowed
     * @deprecated Use validateContextAccess(branchId, "healthcare") instead
     * @param branchId The branch ID (as UUID) to validate access for
     * @throws RuntimeException if access is not allowed
     */
    @Deprecated
    public void validateBranchAccess(UUID branchId) {
        if (!hasBranchAccess(branchId)) {
            throw new RuntimeException("Access denied: You do not have access to this branch");
        }
    }
}