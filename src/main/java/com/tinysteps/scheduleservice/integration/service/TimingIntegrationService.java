package com.tinysteps.scheduleservice.integration.service;

import com.tinysteps.scheduleservice.config.IntegrationProperties;
import com.tinysteps.scheduleservice.exception.IntegrationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimingIntegrationService {

    private final WebClient secureWebClient;
    private final IntegrationProperties integrationProperties;
    private final CircuitBreaker timingServiceCircuitBreaker;

    public boolean isSlotAvailable(UUID doctorId, UUID practiceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        String url = integrationProperties.getTimingService().getBaseUrl()
                + "/doctors/" + doctorId
                + "/practices/" + practiceId
                + "/slots/available?"
                + "date=" + date
                + "&startTime=" + startTime
                + "&endTime=" + endTime;

        return secureWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status.isError(),
                        resp -> Mono.error(new IntegrationException("Error checking slot availability")))
                .bodyToMono(Boolean.class)
                .transformDeferred(CircuitBreakerOperator.of(timingServiceCircuitBreaker))
                .blockOptional()
                .orElse(false);
    }
}
