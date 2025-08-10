package com.tinysteps.scheduleservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker doctorServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-doctor-service");
    }
    @Bean
    public Retry doctorServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-doctor-service");
    }
    @Bean
    public TimeLimiter doctorServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-doctor-service");
    }

    @Bean
    public CircuitBreaker addressServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-address-service");
    }
    @Bean
    public Retry addressServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-address-service");
    }
    @Bean
    public TimeLimiter addressServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-address-service");
    }

    @Bean
    public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-user-service");
    }
    @Bean
    public Retry userServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-user-service");
    }
    @Bean
    public TimeLimiter userServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-user-service");
    }

    @Bean
    public CircuitBreaker timingServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-timing-service");
    }
    @Bean
    public Retry timingServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-timing-service");
    }
    @Bean
    public TimeLimiter timingServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-timing-service");
    }

    @Bean
    public CircuitBreaker sessionServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-session-service");
    }
    @Bean
    public Retry sessionServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-session-service");
    }
    @Bean
    public TimeLimiter sessionServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-session-service");
    }
}
