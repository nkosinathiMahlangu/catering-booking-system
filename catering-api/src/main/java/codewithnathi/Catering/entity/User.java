package codewithnathi.Catering.entity;

import codewithnathi.Catering.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing a registered user of the catering system.
 * <p>
 * This is a pure database entity — it has no Spring Security dependencies.
 * Spring Security integration is handled by {@link codewithnathi.Catering.security.UserPrincipal},
 * which wraps this entity and implements {@code UserDetails}.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    /** Stored as a BCrypt hash — never the plain-text password. */
    private String password;

    private String name;

    private String surname;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** False means the account is disabled; login will be refused. */
    private boolean isActive;

    /** Plain-text OTP stored temporarily during a password-reset flow. */
    private String resetOtpCode;

    /** Expiry time for the current OTP — 2 minutes after generation. */
    private LocalDateTime otpExpiry;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
