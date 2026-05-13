package com.flowboard.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Bulletproof CORS rewriter — runs at LOWEST_PRECEDENCE so it executes LAST in the
 * response chain, AFTER any downstream service has already written its (potentially wrong)
 * Access-Control-Allow-Origin header.
 *
 * It unconditionally strips and rewrites all CORS response headers to safe values derived
 * from the incoming request's Origin (if whitelisted). This guarantees that even if a
 * stale microservice JAR or a misconfigured Spring Security chain leaks an
 * "Access-Control-Allow-Origin: http://localhost:8081" header, it cannot reach the browser.
 */
@Component
public class CorsResponseRewriteFilter implements GlobalFilter, Ordered {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:4200"
    );

    private static final Set<String> CORS_HEADERS = Set.of(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Credentials",
            "Access-Control-Expose-Headers",
            "Access-Control-Max-Age",
            "Vary"
    );

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsCsv;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
            String origin = exchange.getRequest().getHeaders().getOrigin();

            // Strip any CORS headers that downstream services or stale builds may have added.
            CORS_HEADERS.forEach(responseHeaders::remove);

            List<String> allowedOrigins = (allowedOriginsCsv == null || allowedOriginsCsv.isBlank())
                    ? DEFAULT_ALLOWED_ORIGINS
                    : Arrays.stream(allowedOriginsCsv.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .toList();

            if (origin != null && allowedOrigins.contains(origin)) {
                responseHeaders.set("Access-Control-Allow-Origin", origin);
                responseHeaders.set("Access-Control-Allow-Credentials", "true");
                responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
                responseHeaders.set("Access-Control-Allow-Headers", "*");
                responseHeaders.set("Access-Control-Expose-Headers", "Authorization, Content-Type, X-Total-Count");
                responseHeaders.set("Access-Control-Max-Age", "3600");
                responseHeaders.set("Vary", "Origin");
            }
        }));
    }

    @Override
    public int getOrder() {
        // Run LAST in the response chain — after every other filter has finished writing headers.
        return Ordered.LOWEST_PRECEDENCE;
    }

    // expose for testing
    List<String> resolvedAllowedOrigins() {
        return Collections.unmodifiableList(DEFAULT_ALLOWED_ORIGINS);
    }
}
