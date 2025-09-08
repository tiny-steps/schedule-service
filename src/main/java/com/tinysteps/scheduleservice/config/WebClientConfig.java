package com.tinysteps.scheduleservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .defaultHeader("X-Internal-Secret", internalApiSecret)
                .filter(logRequestHeaders());
    }

    @Bean
    public WebClient publicWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
        return loadBalancedWebClientBuilder.build();
    }

    @Bean
    public WebClient secureWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
        return loadBalancedWebClientBuilder
                .filter(jwtPropagationFilter())
                .build();
    }

    private ExchangeFilterFunction jwtPropagationFilter() {
        return (request, next) -> ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {
                    Authentication authentication = context.getAuthentication();
                    if (authentication != null && authentication.getCredentials() instanceof String jwt) {
                        ClientRequest authorizedRequest = ClientRequest.from(request)
                                .headers(headers -> headers.setBearerAuth(jwt))
                                .build();
                        return next.exchange(authorizedRequest);
                    }
                    return next.exchange(request);
                })
                .switchIfEmpty(next.exchange(request));
    }

    private ExchangeFilterFunction logRequestHeaders() {
        return (request, next) -> {
            logger.info("================ Outgoing Request from Schedule-Service =================");
            logger.info("Request: {} {}", request.method(), request.url());
            request.headers().forEach((name, values) -> {
                if ("X-Internal-Secret".equals(name)) {
                    logger.info("Header: {}=[MASKED]", name);
                } else {
                    logger.info("Header: {}={}", name, values);
                }
            });
            logger.info("======================================================================");
            return next.exchange(request);
        };
    }
}
