package codewithnathi.Catering.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * General application beans that must live outside {@link SecurityConfig}.
 * <p>
 * {@code PasswordEncoder} is defined here to break the circular dependency that
 * would occur if it were defined in SecurityConfig:
 * <pre>
 *   SecurityConfig → UserService → PasswordEncoder → SecurityConfig  ← cycle!
 * </pre>
 * Placing it here means SecurityConfig and UserService both depend on AppConfig,
 * which has no upstream Spring Security dependencies.
 * </p>
 */
@Configuration
public class AppConfig {

    /**
     * BCrypt password encoder with default strength (10 rounds).
     * Injected into {@link codewithnathi.Catering.service.UserService} and
     * {@link SecurityConfig} via constructor injection.
     *
     * @return a BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
