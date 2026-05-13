package com.flowboard.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path   = request.getURI().getPath();
        String query  = request.getURI().getQuery();
        String fullPath = query != null ? path + "?" + query : path;

        log.info("→ GATEWAY IN  | {} {} | from: {}",
                method,
                fullPath,
                request.getRemoteAddress() != null
                        ? request.getRemoteAddress().getHostString()
                        : "unknown");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;

            String level = statusCode >= 500 ? "ERROR"
                    : statusCode >= 400 ? "WARN"
                    : "INFO";

            if ("ERROR".equals(level)) {
                log.error("← GATEWAY OUT | {} {} | status={} | {}ms", method, fullPath, statusCode, duration);
            } else if ("WARN".equals(level)) {
                log.warn("← GATEWAY OUT | {} {} | status={} | {}ms", method, fullPath, statusCode, duration);
            } else {
                log.info("← GATEWAY OUT | {} {} | status={} | {}ms", method, fullPath, statusCode, duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        // Run before all other filters
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
