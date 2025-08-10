package com.tinysteps.scheduleservice.integration.service;

import com.tinysteps.scheduleservice.config.IntegrationProperties;
import com.tinysteps.scheduleservice.exception.IntegrationException;
import com.tinysteps.scheduleservice.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserIntegrationService {

    private final WebClient secureWebClient;
    private final IntegrationProperties integrationProperties;
    private final CircuitBreaker userServiceCircuitBreaker;

    public void validateUserExistsOrThrow(UUID userId) {
        String url = integrationProperties.getUserService().getBaseUrl() + "/" + userId;
        secureWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        resp -> Mono.error(new ResourceNotFoundException("User not found: " + userId))
                )
                .onStatus(
                        status -> status.isError(),
                        resp -> Mono.error(new IntegrationException("Error calling User Service"))
                )
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(userServiceCircuitBreaker))
                .block();
    }

    public boolean validateUserOwnership(UUID userId, String authUserId) {
        String url = integrationProperties.getUserService().getBaseUrl()
                + "/" + userId + "/owner?userId=" + authUserId;
        return secureWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        resp -> Mono.error(new IntegrationException("Error checking user ownership"))
                )
                .bodyToMono(Boolean.class)
                .transformDeferred(CircuitBreakerOperator.of(userServiceCircuitBreaker))
                .blockOptional()
                .orElse(false);
    }
}
