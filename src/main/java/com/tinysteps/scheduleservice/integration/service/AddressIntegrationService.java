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
public class AddressIntegrationService {

    private final WebClient secureWebClient;
    private final IntegrationProperties integrationProperties;
    private final CircuitBreaker addressServiceCircuitBreaker;

    public void validatePracticeExistsOrThrow(UUID practiceId) {
        String url = integrationProperties.getAddressService().getBaseUrl() + "/" + practiceId;
        secureWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        resp -> Mono.error(new ResourceNotFoundException("Practice not found: " + practiceId)))
                .onStatus(status -> status.isError(),
                        resp -> Mono.error(new IntegrationException("Error calling Address Service")))
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(addressServiceCircuitBreaker))
                .block();
    }
}
