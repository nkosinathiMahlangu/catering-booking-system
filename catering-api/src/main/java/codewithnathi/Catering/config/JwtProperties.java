package codewithnathi.Catering.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds JWT configuration values from application.yaml (prefix: jwt).
 * <p>
 * {@code jwt.secret}     — Base64-encoded HMAC-SHA256 signing key (injected from env JWT_SECRET).
 * {@code jwt.expiration} — Token lifetime in milliseconds (default 86400000 = 24 h).
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /** Base64-encoded secret used to sign and verify JWT tokens. */
    private String secret;

    /** Token validity duration in milliseconds. */
    private long expiration;
}
