package com.flowboard.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/gateway")
public class HealthController {

    private final WebClient webClient;

    // Eureka-resolved service URLs — no hardcoded ports
    private static final Map<String, String> SERVICES = new LinkedHashMap<>(Map.of(
            "auth-service",         "http://AUTH-SERVICE",
            "workspace-service",    "http://WORKSPACE-SERVICE",
            "board-service",        "http://BOARD-SERVICE",
            "list-service",         "http://LIST-SERVICE",
            "card-service",         "http://CARD-SERVICE",
            "comment-service",      "http://COMMENT-SERVICE",
            "label-service",        "http://LABEL-SERVICE",
            "notification-service", "http://NOTIFICATION-SERVICE"
    ));

    public HealthController(@LoadBalanced WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Configuration
    static class WebClientConfig {
        @Bean
        @LoadBalanced
        public WebClient.Builder loadBalancedWebClientBuilder() {
            return WebClient.builder();
        }
    }

    // ───────── BASIC GATEWAY HEALTH ─────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> gatewayHealth() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("gateway", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("port", 8080);
        response.put("message", "FlowBoard API Gateway is running");
        response.put("routes", SERVICES);

        return ResponseEntity.ok(response);
    }

    // ───────── REAL SERVICES HEALTH CHECK ─────────
    @GetMapping("/services/status")
    public Mono<ResponseEntity<Map<String, Object>>> servicesStatus() {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());

        return Flux.fromIterable(SERVICES.entrySet())
                .flatMap(entry ->
                        webClient.get()
                                .uri(entry.getValue() + "/actuator/health")
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(res -> Map.entry(entry.getKey(), "UP"))
                                .onErrorResume(e -> {
                                    log.warn("Service DOWN: {}", entry.getKey());
                                    return Mono.just(Map.entry(entry.getKey(), "DOWN"));
                                })
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(statusMap -> {
                    result.put("services", statusMap);
                    result.put("message", "Service health check completed");
                    return ResponseEntity.ok(result);
                });
    }
}