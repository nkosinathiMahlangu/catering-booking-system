package codewithnathi.Catering.service;

import codewithnathi.Catering.dto.RegisterRequest;
import codewithnathi.Catering.dto.ResetPasswordRequest;
import codewithnathi.Catering.entity.User;
import codewithnathi.Catering.enums.Role;
import codewithnathi.Catering.repository.UserRepository;
import codewithnathi.Catering.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Core user management service. Implements {@link UserDetailsService} so Spring
 * Security can load users during JWT filter authentication and login.
 * <p>
 * {@code loadUserByUsername} returns a {@link UserPrincipal} adapter rather than
 * the raw {@link User} entity, keeping the entity free of Spring Security contracts.
 * </p>
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>User registration with password validation and uniqueness enforcement</li>
 *   <li>OTP generation and email dispatch for password reset</li>
 *   <li>OTP verification and secure password update</li>
 *   <li>Lookup of the currently authenticated user by email</li>
 * </ul>
 * </p>
 * <p>
 * {@code PasswordEncoder} is injected from {@link codewithnathi.Catering.config.AppConfig}
 * (NOT from SecurityConfig) to avoid a circular bean dependency.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordValidatorService passwordValidatorService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Loads a user by email for Spring Security authentication.
     * Returns a {@link UserPrincipal} wrapping the entity so Spring Security's
     * contracts are satisfied without polluting the JPA entity.
     *
     * @param email the email address acting as the username
     * @return a {@link UserPrincipal} wrapping the matching {@link User}
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new UserPrincipal(user);
    }

    /**
     * Registers a new user account.
     * <p>
     * Steps:
     * <ol>
     *   <li>Rejects duplicate emails (would violate the DB unique constraint)</li>
     *   <li>Delegates password strength + personal-info checks to {@link PasswordValidatorService}</li>
     *   <li>BCrypt-encodes the password before persisting</li>
     *   <li>Assigns the default role USER and marks the account active</li>
     * </ol>
     * </p>
     *
     * @param req registration payload (email, plain-text password, name, surname)
     * @return the saved {@link User} entity
     * @throws IllegalArgumentException if the email is already taken or the password fails validation
     */
    public User register(RegisterRequest req) {
        // Guard against duplicate accounts before any expensive operations
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered: " + req.getEmail());
        }

        // Enforce strength rules and personal-info check
        passwordValidatorService.validate(req.getPassword(), req.getName(), req.getSurname());

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .surname(req.getSurname())
                .role(Role.USER)   // all self-registered users get USER; ADMIN is assigned manually
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * Initiates a password reset flow by generating a 6-digit OTP, persisting its
     * SHA-256 hash against the user, and emailing the plain OTP.
     * <p>
     * Silently no-ops for unknown email addresses to prevent account enumeration
     * (the caller always receives a 200 response regardless).
     * </p>
     *
     * @param email the email address of the account to reset
     */
    public void initiatePasswordReset(String email) {
        // Silent no-op for unknown emails — prevents user-existence enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            // SecureRandom ensures the OTP cannot be predicted; formatted to always 6 digits
            String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            user.setResetOtpCode(hashOtp(otp));
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(2));
            userRepository.save(user);
            emailService.sendOtpEmail(email, otp);
        });
    }

    /**
     * Completes the password reset after the user has verified their OTP.
     * <p>
     * Validates the OTP expiry and value before applying the new password.
     * Clears OTP fields after a successful reset to prevent replay attacks.
     * </p>
     *
     * @param req contains email, the submitted OTP code, and the desired new password
     * @throws UsernameNotFoundException if the account does not exist
     * @throws IllegalArgumentException  if the OTP has expired, is incorrect, or the new password fails validation
     */
    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + req.getEmail()));

        // Reject if OTP was never set or has passed its 2-minute window
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (!hashOtp(req.getOtpCode()).equals(user.getResetOtpCode())) {
            throw new IllegalArgumentException("Invalid OTP code.");
        }

        // Apply the same password strength rules as during registration
        passwordValidatorService.validate(req.getNewPassword(), user.getName(), user.getSurname());

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        // Clear OTP fields so they cannot be reused
        user.setResetOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }

    /**
     * Retrieves the full {@link User} entity for the currently authenticated user.
     * Used by downstream services that need user details beyond what
     * {@link org.springframework.security.core.Authentication} provides.
     *
     * @param email the email from the JWT subject claim
     * @return the matching {@link User} entity
     * @throws UsernameNotFoundException if the account no longer exists
     */
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the hex-encoded SHA-256 hash of the plain OTP value.
     * Used to avoid storing the raw OTP in the database.
     */
    public String hashOtp(String plainOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainOtp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on all JVMs
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
