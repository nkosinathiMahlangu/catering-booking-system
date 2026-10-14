package codewithnathi.Catering.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS policy for the catering API.
 * <p>
 * Allowed origins are read from the {@code CORS_ORIGINS} environment variable
 * (comma-separated list), defaulting to {@code http://localhost:3000} for local
 * frontend development. Credentials are enabled so the browser includes cookies
 * and authorization headers on cross-origin requests.
 * </p>
 */
@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins.
     * Override with {@code CORS_ORIGINS} env var in production.
     */
    @Value("${CORS_ORIGINS:http://localhost:3000}")
    private String corsOrigins;

    /**
     * Registers a global CORS policy applied to all API routes.
     *
     * @return a {@link CorsConfigurationSource} consumed by Spring Security's cors() DSL
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Split in case multiple origins are provided as a comma-separated env var
        config.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Required for cookies/Authorization header to be sent by the browser
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
