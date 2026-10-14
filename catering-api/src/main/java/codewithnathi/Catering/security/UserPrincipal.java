package codewithnathi.Catering.security;

import codewithnathi.Catering.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter that wraps the {@link User} JPA entity and implements Spring Security's
 * {@link UserDetails} contract.
 * <p>
 * Keeping this adapter separate from the {@link User} entity enforces separation of
 * concerns — the entity handles persistence, this class handles authentication.
 * Use {@link #getUser()} wherever the underlying entity (e.g. its {@code id} or
 * {@code role}) is needed beyond what {@link UserDetails} exposes.
 * </p>
 */
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    /**
     * Exposes the wrapped {@link User} entity for cases where entity-level data
     * (id, role, name) is needed after authentication — e.g. building an AuthResponse.
     *
     * @return the underlying {@link User} entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Maps the user's {@link codewithnathi.Catering.enums.Role} to a Spring Security
     * {@code ROLE_} authority (e.g. {@code ROLE_ADMIN}, {@code ROLE_USER}).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** The username in Spring Security terms is the user's email address. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Delegates to {@link User#isActive()} — disabled accounts cannot log in. */
    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
