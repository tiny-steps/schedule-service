package com.tinysteps.scheduleservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String secretHeader = request.getHeader(INTERNAL_SECRET_HEADER);

        // Only act if the secret header is present and valid
        if (internalApiSecret.equals(secretHeader)) {
            // Create an authentication object for our trusted "internal-service" principal
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "internal-service",
                    null,
                    // Grant the ADMIN role, which will pass @PreAuthorize checks
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set this fully authorized principal in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}