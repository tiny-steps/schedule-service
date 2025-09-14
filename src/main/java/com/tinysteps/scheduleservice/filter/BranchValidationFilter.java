package com.tinysteps.scheduleservice.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinysteps.scheduleservice.service.SecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchValidationFilter extends OncePerRequestFilter {

    private final SecurityService securityService;
    private final ObjectMapper objectMapper;

    // Patterns for endpoints that require branch validation
    private static final List<Pattern> BRANCH_REQUIRED_PATTERNS = List.of(
        Pattern.compile("/api/v1/appointments.*"),
        Pattern.compile("/api/v1/schedules.*")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (!requiresBranchValidation(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Handle branchId=all (admin scope) via query param or path /branch/all
        String branchParam = request.getParameter("branchId");
        boolean pathAll = requestURI.matches(".*/branch/all(/.*)?");
        if ((branchParam != null && branchParam.equalsIgnoreCase("all")) || pathAll) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
            if (!isAdmin) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied: Only ADMIN can query all branches");
                return;
            }
            request.setAttribute("branchScope", "ALL");
            filterChain.doFilter(request, response);
            return;
        }

        // Skip branch validation if principal is not a Jwt but has ADMIN role (internal-service synthetic auth)
        if (!(authentication.getPrincipal() instanceof Jwt) && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"))) {
            log.debug("Skipping branch validation for non-JWT ADMIN principal: {} (type: {})", authentication.getName(), authentication.getPrincipal().getClass().getSimpleName());
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToUse = request;
        byte[] cachedBody = null;
        boolean bodyCandidate = ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()));
        if (bodyCandidate) {
            try {
                cachedBody = request.getInputStream().readAllBytes();
                requestToUse = new CachedBodyRequestWrapper(request, cachedBody);
            } catch (IOException e) {
                log.warn("Unable to cache request body: {}", e.getMessage());
            }
        }

        try {
            // Derive roles defensively from authorities to avoid SecurityService throwing when principal not Jwt yet
            List<String> userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                    .collect(Collectors.toList());
            // Check if user has required roles
            if (!hasRequiredRole(userRoles)) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied: Insufficient privileges");
                return;
            }

            // Extract branchId from request
            String branchId = extractBranchId(requestToUse, cachedBody);

            if (branchId != null) {
                // Validate branch access
                securityService.validateBranchAccess(branchId);
                log.debug("Branch validation successful for branchId: {}", branchId);
            } else {
                // Use primary branch if no branchId specified
                UUID primaryBranchId = securityService.getPrimaryBranchId();
                if (primaryBranchId != null) {
                    requestToUse.setAttribute("branchId", primaryBranchId.toString());
                    log.debug("Using primary branch: {}", primaryBranchId);
                }
            }

            filterChain.doFilter(requestToUse, response);

        } catch (RuntimeException ex) {
            log.warn("Branch validation failed: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.FORBIDDEN, ex.getMessage());
        }
    }

    private boolean requiresBranchValidation(String requestURI) {
        return BRANCH_REQUIRED_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(requestURI).matches());
    }

    private boolean hasRequiredRole(List<String> userRoles) {
        return userRoles.contains("ADMIN") ||
               userRoles.contains("DOCTOR") ||
               userRoles.contains("RECEPTIONIST");
    }

    private String extractBranchId(HttpServletRequest request, byte[] cachedBody) {
        // First, try to get from query parameters
        String branchId = request.getParameter("branchId");
        if (branchId != null && !branchId.isEmpty()) {
            return branchId;
        }

        // Then, try to extract from path variables
        branchId = extractFromPathVariable(request.getRequestURI());
        if (branchId != null) {
            return branchId;
        }

        // Finally, try to extract from request body (for POST/PUT requests)
        if (cachedBody != null && cachedBody.length > 0) {
            try {
                JsonNode node = objectMapper.readTree(cachedBody);
                JsonNode branchIdNode = node.get("branchId");
                if (branchIdNode != null && !branchIdNode.isNull()) {
                    return branchIdNode.asText();
                }
            } catch (Exception e) {
                log.debug("Unable to parse body for branchId: {}", e.getMessage());
            }
        }

        return null;
    }

    private String extractFromPathVariable(String requestURI) {
        // Extract branchId from path patterns like /api/v1/branches/{branchId}/...
        Pattern pattern = Pattern.compile("/branches/([a-fA-F0-9-]{36})");
        Matcher matcher = pattern.matcher(requestURI);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String errorResponse = String.format(
            "{\"error\": \"%s\", \"message\": \"%s\", \"status\": %d}",
            status.getReasonPhrase(),
            message,
            status.value()
        );

        response.getWriter().write(errorResponse);
    }

    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body != null ? body : new byte[0];
        }
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) { }
            };
        }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
