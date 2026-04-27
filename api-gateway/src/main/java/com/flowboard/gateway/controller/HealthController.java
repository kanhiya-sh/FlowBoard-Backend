package com.flowboard.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${services.auth.url:http://localhost:8081}")
    private String authUrl;

    @Value("${services.workspace.url:http://localhost:8082}")
    private String workspaceUrl;

    @Value("${services.board.url:http://localhost:8083}")
    private String boardUrl;

    @Value("${services.list.url:http://localhost:8084}")
    private String listUrl;

    @Value("${services.card.url:http://localhost:8085}")
    private String cardUrl;

    @Value("${services.comment.url:http://localhost:8086}")
    private String commentUrl;

    @Value("${services.label.url:http://localhost:8087}")
    private String labelUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationUrl;

    public HealthController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // ───────── BASIC GATEWAY HEALTH ─────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> gatewayHealth() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("gateway", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("port", 8080);
        response.put("message", "FlowBoard API Gateway is running");

        response.put("routes", Map.of(
                "auth-service", authUrl,
                "workspace-service", workspaceUrl,
                "board-service", boardUrl,
                "list-service", listUrl,
                "card-service", cardUrl,
                "comment-service", commentUrl,
                "label-service", labelUrl,
                "notification-service", notificationUrl
        ));

        return ResponseEntity.ok(response);
    }

    // ───────── REAL SERVICES HEALTH CHECK ─────────
    @GetMapping("/services/status")
    public Mono<ResponseEntity<Map<String, Object>>> servicesStatus() {

        Map<String, String> services = new LinkedHashMap<>();
        services.put("auth-service", authUrl);
        services.put("workspace-service", workspaceUrl);
        services.put("board-service", boardUrl);
        services.put("list-service", listUrl);
        services.put("card-service", cardUrl);
        services.put("comment-service", commentUrl);
        services.put("label-service", labelUrl);
        services.put("notification-service", notificationUrl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());

        return Flux.fromIterable(services.entrySet())
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