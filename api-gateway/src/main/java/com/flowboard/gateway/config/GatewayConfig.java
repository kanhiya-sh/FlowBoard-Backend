package com.flowboard.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayConfig {

    @Value("${services.auth.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.workspace.url:http://localhost:8082}")
    private String workspaceServiceUrl;

    @Value("${services.board.url:http://localhost:8083}")
    private String boardServiceUrl;

    @Value("${services.list.url:http://localhost:8084}")
    private String listServiceUrl;

    @Value("${services.card.url:http://localhost:8085}")
    private String cardServiceUrl;

    @Value("${services.comment.url:http://localhost:8086}")
    private String commentServiceUrl;

    @Value("${services.label.url:http://localhost:8087}")
    private String labelServiceUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Registering FlowBoard Gateway routes...");
        log.info("  auth        → {}", authServiceUrl);
        log.info("  workspace   → {}", workspaceServiceUrl);
        log.info("  board       → {}", boardServiceUrl);
        log.info("  list        → {}", listServiceUrl);
        log.info("  card        → {}", cardServiceUrl);
        log.info("  comment     → {}", commentServiceUrl);
        log.info("  label       → {}", labelServiceUrl);
        log.info("  notification→ {}", notificationServiceUrl);

        return builder.routes()

                // ── Auth Service ─────────────────────────────────────────
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(authServiceUrl))

                // ── Workspace Service ────────────────────────────────────
                .route("workspace-service", r -> r
                        .path("/workspaces/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(workspaceServiceUrl))

                // ── Board Service ────────────────────────────────────────
                .route("board-service", r -> r
                        .path("/boards/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(boardServiceUrl))

                // ── List Service ─────────────────────────────────────────
                .route("list-service", r -> r
                        .path("/lists/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(listServiceUrl))

                // ── Card Service ─────────────────────────────────────────
                .route("card-service", r -> r
                        .path("/cards/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(cardServiceUrl))

                // ── Comment Service ──────────────────────────────────────
                .route("comment-service", r -> r
                        .path("/comments/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(commentServiceUrl))

                // ── Attachment Service (same port as comment) ────────────
                .route("attachment-service", r -> r
                        .path("/attachments/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(commentServiceUrl))

                // ── Label Service ────────────────────────────────────────
                .route("label-service", r -> r
                        .path("/labels/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(labelServiceUrl))

                // ── Checklist Service (same port as label) ───────────────
                .route("checklist-service", r -> r
                        .path("/checklists/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(labelServiceUrl))

                // ── Notification Service ─────────────────────────────────
                .route("notification-service", r -> r
                        .path("/notifications/**")
                        .filters(f -> f
                                .preserveHostHeader()
                        )
                        .uri(notificationServiceUrl))

                .build();
    }
}
