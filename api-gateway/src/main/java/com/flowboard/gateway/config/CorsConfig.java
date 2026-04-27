package com.flowboard.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.*;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed origins — add production URL here when deploying
        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",   // Angular dev
                "http://localhost:3000"    // React dev (future)
        ));

        // All HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // All headers — especially Authorization (JWT)
        corsConfig.setAllowedHeaders(Collections.singletonList("*"));

        // Allow cookies / Authorization header
        corsConfig.setAllowCredentials(true);

        // Preflight cache duration — 1 hour
        corsConfig.setMaxAge(3600L);

        // Expose these headers to the browser
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * Handle OPTIONS preflight requests explicitly.
     * Returns 200 immediately for all OPTIONS requests so browser doesn't wait.
     */
    @Bean
    public WebFilter optionsFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (request.getMethod() == HttpMethod.OPTIONS) {
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();
                headers.add("Access-Control-Allow-Origin", "http://localhost:4200");
                headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
                headers.add("Access-Control-Allow-Headers", "*");
                headers.add("Access-Control-Allow-Credentials", "true");
                headers.add("Access-Control-Max-Age", "3600");
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
            return chain.filter(exchange);
        };
    }
}
