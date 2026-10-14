package codewithnathi.Catering.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility for generating and validating JWT tokens using JJWT 0.12.x.
 * <p>
 * All tokens are signed with HMAC-SHA256. The signing key is derived from
 * the Base64-encoded secret in {@link JwtProperties}.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * Derives the HMAC-SHA256 signing key from the Base64-encoded secret.
     * The key is reconstructed on each call; caching is intentionally avoided
     * to simplify rotation if the secret changes.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userDetails the authenticated user; {@code getUsername()} is used as the subject
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())          // email as subject
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(getSigningKey())                   // JJWT 0.12.x auto-selects HS256
                .compact();
    }

    /**
     * Extracts the subject (email) from a JWT without validating expiry.
     *
     * @param token a compact JWT string
     * @return the email address stored as the token subject
     * @throws io.jsonwebtoken.JwtException if the token is malformed or the signature is invalid
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates that the token belongs to the given user and has not expired.
     *
     * @param token       a compact JWT string
     * @param userDetails the user to match against
     * @return {@code true} if the token is valid for this user, {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            // Both conditions must hold: subject matches AND token is not past its expiry
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            // Any parsing/signature exception means the token is not trustworthy
            return false;
        }
    }

    /**
     * Checks whether the token's expiration timestamp is in the past.
     *
     * @param token a compact JWT string (assumed to have a valid signature)
     * @return {@code true} if expired
     */
    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiration.before(new Date());
    }
}
