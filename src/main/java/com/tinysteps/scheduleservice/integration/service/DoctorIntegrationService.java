package com.tinysteps.scheduleservice.integration.service;

import com.tinysteps.scheduleservice.config.IntegrationProperties;
import com.tinysteps.scheduleservice.exception.IntegrationException;
import com.tinysteps.scheduleservice.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorIntegrationService {

    private final WebClient secureWebClient;
    private final IntegrationProperties integrationProperties;
    private final CircuitBreaker doctorServiceCircuitBreaker;

    public void validateDoctorExistsOrThrow(UUID doctorId) {
        String url = integrationProperties.getDoctorService().getBaseUrl() + "/" + doctorId;
        secureWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        resp -> Mono.error(new ResourceNotFoundException("Doctor not found: " + doctorId))
                )
                .onStatus(
                        HttpStatusCode::isError,
                        resp -> Mono.error(new IntegrationException("Error calling Doctor Service"))
                )
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(doctorServiceCircuitBreaker))
                .block();
    }

    public boolean validateDoctorOwnership(UUID doctorId, String userId) {
        String url = integrationProperties.getDoctorService().getBaseUrl()
                + "/" + doctorId + "/owner?userId=" + userId;
        return secureWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        resp -> Mono.error(new IntegrationException("Error checking doctor ownership"))
                )
                .bodyToMono(Boolean.class)
                .transformDeferred(CircuitBreakerOperator.of(doctorServiceCircuitBreaker))
                .blockOptional()
                .orElse(false);
    }
}
