package com.flowboard.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for CORS at the API gateway.
 *
 * Hardening rules (so we never see "Access-Control-Allow-Origin: http://localhost:8761" again):
 *   1. Origins are env-driven via CORS_ALLOWED_ORIGINS — defaults cover Angular dev + CRA dev.
 *   2. setAllowedOriginPatterns is used (works correctly with allowCredentials=true).
 *   3. Bean is ordered HIGHEST_PRECEDENCE so it runs before any other web filter.
 *   4. Gateway also uses DedupeResponseHeader=RETAIN_LAST to overwrite any stray
 *      downstream CORS header (see application.properties).
 */
@Configuration
public class CorsConfig {

    private static final List<String> DEFAULT_ORIGIN_PATTERNS = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:4200"
    );

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsCsv;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        List<String> originPatterns = (allowedOriginsCsv == null || allowedOriginsCsv.isBlank())
                ? DEFAULT_ORIGIN_PATTERNS
                : Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();

        // setAllowedOriginPatterns (NOT setAllowedOrigins) — required when allowCredentials=true
        corsConfig.setAllowedOriginPatterns(originPatterns);
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        corsConfig.setAllowedHeaders(Collections.singletonList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Total-Count"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }
}