package codewithnathi.Catering.config;

import codewithnathi.Catering.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the catering API.
 * <p>
 * Key decisions:
 * <ul>
 *   <li><b>Stateless sessions</b> — no HttpSession is created; all state lives in the JWT.</li>
 *   <li><b>CSRF disabled</b> — safe for stateless REST APIs that don't use cookies for auth.</li>
 *   <li><b>PasswordEncoder injected from AppConfig</b> — avoids the circular dependency:
 *       SecurityConfig → UserService → PasswordEncoder → SecurityConfig.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Injected from {@link AppConfig} — NOT defined here to avoid circular dependency.
     * SecurityConfig depends on AppConfig; AppConfig has no dependency on SecurityConfig.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Wires UserDetailsService + PasswordEncoder into a DaoAuthenticationProvider.
     * Spring Security 6 requires {@link UserService} to be passed via the constructor.
     *
     * @return a configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean so {@link codewithnathi.Catering.service.AuthService}
     * can programmatically authenticate login requests.
     *
     * @param config provided by Spring Security's auto-configuration
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if the manager cannot be resolved
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the HTTP security filter chain for all API requests.
     * <p>
     * Authorization rules (evaluated top-to-bottom, first match wins):
     * <ol>
     *   <li>{@code POST /api/auth/**} — public (registration, login, OTP endpoints)</li>
     *   <li>{@code GET /api/packages} — public (customers can browse packages without logging in)</li>
     *   <li>{@code POST/PUT/DELETE /api/packages/**} — ADMIN only (manage catering packages)</li>
     *   <li>{@code GET /api/admin/**} — ADMIN only (admin dashboards and reports)</li>
     *   <li>All other {@code /api/**} — authenticated users (bookings, payments, profile)</li>
     * </ol>
     * </p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is unnecessary for stateless JWT APIs (no session cookies)
                .csrf(AbstractHttpConfigurer::disable)

                // Apply the CORS policy defined in CorsConfig
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // No server-side session — each request must carry its own JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints are open — users need to register and log in without a token
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()

                        // Public package listing — anonymous browsing is allowed
                        .requestMatchers(HttpMethod.GET, "/api/packages").permitAll()

                        // Package mutations are admin-only (create, update, deactivate)
                        .requestMatchers(HttpMethod.POST, "/api/packages/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/packages/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/packages/**").hasRole("ADMIN")

                        // Admin management endpoints
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasRole("ADMIN")

                        // Everything else under /api/** requires a valid token
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())

                // JWT filter runs before Spring's UsernamePasswordAuthenticationFilter
                // so the SecurityContext is populated before authorization checks happen
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
